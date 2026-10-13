package ru.landilf.dactune.data.audio

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.landilf.dactune.domain.model.*
import ru.landilf.dactune.domain.repository.PlaybackRepository
import ru.landilf.dactune.platform.AdaptiveKeepAliveService
import ru.landilf.dactune.R

/** Application-scoped session state is shared by the service and all screen instances. */
class AndroidPlaybackRepository(private val context: Context) : PlaybackRepository {
    private val mutableState = MutableStateFlow(PlaybackState())
    override val state = mutableState.asStateFlow()
    private var player: KeepAliveAudioPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var deadline: Long? = null
    private var tick: Runnable? = null
    var sessionGeneration: Long = 0
        private set

    override fun start(settings: KeepAliveSettings) {
        if (state.value.active) return
        require(!settings.adaptiveEnabled) { context.getString(R.string.error_capture_consent_required) }
        begin(settings)
        try {
            player = KeepAliveAudioPlayer(
                settings.signal,
                settings.levelDbfs,
                context
            ).also { it.start() }
            markRunning()
        } catch (error: Exception) {
            stop(StopReason.ERROR, error.message)
        }
    }

    fun startAdaptive(settings: KeepAliveSettings, resultCode: Int, resultData: Intent) {
        if (state.value.active) return
        begin(settings)
        try {
            AdaptiveKeepAliveService.start(context, resultCode, resultData, settings)
        } catch (error: Exception) {
            stop(StopReason.ERROR, error.message)
        }
    }

    private fun begin(settings: KeepAliveSettings) {
        sessionGeneration++
        mutableState.value = PlaybackState(PlaybackPhase.STARTING, settings)
    }

    fun markRunning() {
        if (!state.value.active) return
        mutableState.value = state.value.copy(phase = PlaybackPhase.RUNNING)
        val minutes = state.value.settings?.timerMinutes ?: 0
        if (minutes == 0) return
        deadline = SystemClock.elapsedRealtime() + minutes * 60_000L
        tick = object : Runnable {
            override fun run() {
                val end = deadline ?: return
                val remaining = (end - SystemClock.elapsedRealtime()).coerceAtLeast(0)
                if (remaining == 0L) {
                    stop(StopReason.TIMER)
                    return
                }
                mutableState.value = state.value.copy(remainingSeconds = (remaining + 999) / 1000)
                handler.postDelayed(this, 1000)
            }
        }.also { it.run() }
    }

    override fun stop(reason: StopReason) = stop(reason, null)

    fun stop(reason: StopReason, error: String?) {
        val adaptive = state.value.settings?.adaptiveEnabled == true
        tick?.let(handler::removeCallbacks)
        tick = null
        deadline = null
        player?.stop()
        player = null
        mutableState.value = PlaybackState(stopReason = reason, error = error)
        if (adaptive) context.stopService(Intent(context, AdaptiveKeepAliveService::class.java))
    }

    fun captureEnded(generation: Long, error: String? = null) {
        if (generation == sessionGeneration && state.value.active) {
            stop(if (error == null) StopReason.CAPTURE_ENDED else StopReason.ERROR, error)
        }
    }
}
