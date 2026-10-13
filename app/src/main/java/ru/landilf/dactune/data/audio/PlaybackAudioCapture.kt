package ru.landilf.dactune.data.audio

import android.Manifest
import android.content.Context
import ru.landilf.dactune.R
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.projection.MediaProjection
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import java.util.concurrent.atomic.AtomicBoolean
import ru.landilf.dactune.domain.engine.SignalLevelAnalyzer

@RequiresApi(29)
class PlaybackAudioCapture(private val context: Context) {
    private val running = AtomicBoolean(false)
    private var record: AudioRecord? = null
    private var thread: Thread? = null

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun start(
        projection: MediaProjection, excludedUid: Int, onLevel: (Double) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val minimum = AudioRecord.getMinBufferSize(
            48_000, AudioFormat.CHANNEL_IN_STEREO,
            AudioFormat.ENCODING_PCM_FLOAT
        )
        check(minimum > 0) { context.getString(R.string.error_audio_capture_unavailable) }
        val capture = AudioPlaybackCaptureConfiguration.Builder(projection)
            .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            .excludeUid(excludedUid).build()
        val created = AudioRecord.Builder()
            .setAudioFormat(
                AudioFormat.Builder().setSampleRate(48_000)
                    .setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT).build()
            )
            .setBufferSizeInBytes(maxOf(minimum, 3840 * 4))
            .setAudioPlaybackCaptureConfig(capture).build()
        record = created
        try {
            check(created.state == AudioRecord.STATE_INITIALIZED) {
                context.getString(R.string.error_audio_capture_initialization)
            }
            created.startRecording()
            running.set(true)
            thread = Thread({
                val samples = FloatArray(3840)
                val analyzer = SignalLevelAnalyzer()
                try {
                    while (running.get()) {
                        val count =
                            created.read(samples, 0, samples.size, AudioRecord.READ_BLOCKING)
                        if (!running.get()) break
                        check(count > 0) {
                            context.getString(
                                R.string.error_audio_capture_read,
                                count
                            )
                        }
                        onLevel(analyzer.rmsDbfs(samples, count))
                    }
                } catch (error: Exception) {
                    if (running.get()) onError(error)
                }
            }, "DACtuneAdaptiveAnalyzer").apply { start() }
        } catch (error: Exception) {
            stop()
            throw error
        }
    }

    fun stop() {
        running.set(false)
        // Stop first to unblock READ_BLOCKING, then wait before releasing its native resources.
        record?.let {
            try {
                it.stop()
            } catch (_: IllegalStateException) {
            }
        }
        thread?.let {
            try {
                it.join()
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
        thread = null
        record?.release()
        record = null
    }
}
