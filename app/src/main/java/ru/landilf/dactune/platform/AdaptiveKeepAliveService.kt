package ru.landilf.dactune.platform

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import ru.landilf.dactune.DACtuneApplication
import ru.landilf.dactune.R
import ru.landilf.dactune.data.audio.KeepAliveAudioPlayer
import ru.landilf.dactune.data.audio.PlaybackAudioCapture
import ru.landilf.dactune.domain.engine.AdaptiveSignalPolicy
import ru.landilf.dactune.domain.model.KeepAliveSettings

/** Android lifecycle and consent orchestration; audio processing lives in data/domain. */
class AdaptiveKeepAliveService : Service() {
    private val repository get() = (application as DACtuneApplication).container.androidPlaybackRepository
    private val handler = Handler(Looper.getMainLooper())
    private var player: KeepAliveAudioPlayer? = null
    private var projection: MediaProjection? = null
    private var capture: PlaybackAudioCapture? = null
    private var started = false
    private var generation = -1L
    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            repository.captureEnded(generation); stopSelf()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null || !repository.state.value.active) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (started) return START_NOT_STICKY
        started = true
        generation = repository.sessionGeneration
        try {
            val notification = createNotification()
            if (Build.VERSION.SDK_INT >= 29) {
                startForeground(
                    101,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                )
            } else {
                startForeground(101, notification)
            }
            if (Build.VERSION.SDK_INT < 29) error(getString(R.string.status_adaptive_unsupported))
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) !=
                PackageManager.PERMISSION_GRANTED
            ) error(getString(R.string.status_record_audio_denied))
            val data =
                intent.projectionData() ?: error(getString(R.string.error_capture_consent_required))
            val settings = repository.state.value.settings
                ?: error(getString(R.string.error_no_active_session))
            val manager = getSystemService(MediaProjectionManager::class.java)
            val activeProjection =
                manager.getMediaProjection(intent.getIntExtra("result_code", 0), data)
                    ?: error(getString(R.string.error_projection_unavailable))
            projection = activeProjection
            activeProjection.registerCallback(projectionCallback, handler)
            val activePlayer = KeepAliveAudioPlayer(settings.signal, settings.levelDbfs, this)
            player = activePlayer
            activePlayer.start()
            val policy = AdaptiveSignalPolicy()
            val audioCapture = PlaybackAudioCapture(this)
            capture = audioCapture
            audioCapture.start(
                activeProjection, applicationInfo.uid,
                onLevel = { dbfs ->
                    activePlayer.setTargetLevelDbfs(
                        if (policy.shouldMute(dbfs)) null else settings.levelDbfs
                    )
                },
                onError = { error ->
                    handler.post {
                        repository.captureEnded(
                            generation,
                            error.message
                        )
                    }
                })
            repository.markRunning()
        } catch (error: Exception) {
            repository.captureEnded(generation, error.message ?: getString(R.string.unknown_error))
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        if (Build.VERSION.SDK_INT >= 29) capture?.stop()
        capture = null
        player?.stop()
        player = null
        projection?.unregisterCallback(projectionCallback)
        projection?.stop()
        projection = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        repository.captureEnded(generation)
        super.onDestroy()
    }

    private fun createNotification(): Notification {
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(
                "adaptive_keep_alive", getString(R.string.adaptive_notification_channel),
                NotificationManager.IMPORTANCE_LOW
            )
        )
        val openApp = PendingIntent.getActivity(
            this, 0,
            Intent(this, ru.landilf.dactune.presentation.MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, "adaptive_keep_alive")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(getString(R.string.adaptive_notification_title))
            .setContentText(getString(R.string.adaptive_notification_text))
            .setContentIntent(openApp).setOngoing(true).build()
    }

    @Suppress("DEPRECATION")
    private fun Intent.projectionData(): Intent? =
        if (Build.VERSION.SDK_INT >= 33) getParcelableExtra("result_data", Intent::class.java)
        else getParcelableExtra("result_data")

    companion object {
        fun start(
            context: Context,
            resultCode: Int,
            resultData: Intent,
            settings: KeepAliveSettings
        ) {
            require(settings.adaptiveEnabled)
            ContextCompat.startForegroundService(
                context,
                Intent(context, AdaptiveKeepAliveService::class.java).apply {
                    putExtra("result_code", resultCode)
                    putExtra("result_data", resultData)
                })
        }
    }
}
