package ru.landilf.dactune.data.audio

import ru.landilf.dactune.domain.model.KeepAliveSignal
import ru.landilf.dactune.R
import android.content.Context

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.max
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin

/** Generates and continuously writes the selected media signal to Android audio output. */
class KeepAliveAudioPlayer(
    private val signal: KeepAliveSignal,
    levelDbfs: Double,
    private val context: Context
) {
    companion object {
        private const val SAMPLE_RATE_HZ = 48_000
        private const val CHANNEL_COUNT = 2
        private const val FRAMES_PER_WRITE = 1_920
        private const val BYTES_PER_FLOAT_FRAME = CHANNEL_COUNT * 4
        private const val TONE_FREQUENCY_HZ = 18_000.0
    }

    private val targetAmplitude = AtomicReference(levelToAmplitude(levelDbfs))
    private val running = AtomicBoolean(false)

    @Volatile
    private var writtenAmplitude = targetAmplitude.get()

    /** Digital peak envelope of the most recently written buffer, after amplitude smoothing. */
    val currentLevelDbfs: Double
        get() = if (writtenAmplitude > 0f) {
            20.0 * log10(writtenAmplitude.toDouble())
        } else {
            Double.NEGATIVE_INFINITY
        }

    private var audioTrack: AudioTrack? = null
    private var writerThread: Thread? = null

    fun start() {
        check(running.compareAndSet(false, true)) {
            context.getString(R.string.error_keep_alive_running)
        }

        try {
            val minBufferBytes = AudioTrack.getMinBufferSize(
                SAMPLE_RATE_HZ,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_FLOAT
            )
            check(minBufferBytes > 0) { context.getString(R.string.error_audio_output_unavailable) }

            val track = createAudioTrack(minBufferBytes)
            check(track.state == AudioTrack.STATE_INITIALIZED) {
                context.getString(R.string.error_audio_output_initialization)
            }

            audioTrack = track
            track.play()
            writerThread = Thread(
                { writeSignalLoop(track) },
                "DACtuneKeepAlive"
            ).apply { start() }
        } catch (e: Exception) {
            running.set(false)
            audioTrack?.release()
            audioTrack = null
            throw e
        }
    }

    fun stop() {
        running.set(false)

        writerThread?.let {
            try {
                it.join(500)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
        writerThread = null

        audioTrack?.let { track ->
            try {
                track.pause()
                track.flush()
            } catch (_: IllegalStateException) {
            }
            track.release()
        }
        audioTrack = null
    }

    /** Smoothly changes the generated signal level; null silences it without stopping the track. */
    fun setTargetLevelDbfs(levelDbfs: Double?) {
        targetAmplitude.set(levelDbfs?.let(::levelToAmplitude) ?: 0f)
    }

    private fun createAudioTrack(minBufferBytes: Int): AudioTrack {
        return AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE_HZ)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .build()
            )
            .setBufferSizeInBytes(
                max(minBufferBytes, FRAMES_PER_WRITE * BYTES_PER_FLOAT_FRAME)
            )
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
    }

    private fun writeSignalLoop(track: AudioTrack) {
        val samples = FloatArray(FRAMES_PER_WRITE * CHANNEL_COUNT)
        var noiseState = 0x13579BDF
        var tonePhase = 0.0
        val tonePhaseStep = 2.0 * Math.PI * TONE_FREQUENCY_HZ / SAMPLE_RATE_HZ

        var currentAmplitude = targetAmplitude.get()

        while (running.get()) {
            val requestedAmplitude = targetAmplitude.get()
            when (signal) {
                KeepAliveSignal.TONE_18_KHZ -> {
                    for (frame in 0 until FRAMES_PER_WRITE) {
                        currentAmplitude += (requestedAmplitude - currentAmplitude) * 0.0025f
                        val sample = (sin(tonePhase) * currentAmplitude).toFloat()
                        val index = frame * CHANNEL_COUNT
                        samples[index] = sample
                        samples[index + 1] = sample

                        tonePhase += tonePhaseStep
                        if (tonePhase >= 2.0 * Math.PI) {
                            tonePhase -= 2.0 * Math.PI
                        }
                    }
                }

                KeepAliveSignal.WHITE_NOISE -> {
                    for (index in samples.indices) {
                        currentAmplitude += (requestedAmplitude - currentAmplitude) * 0.00125f
                        noiseState = noiseState * 1_664_525 + 1_013_904_223
                        val normalized =
                            ((noiseState ushr 1).toFloat() / Int.MAX_VALUE) * 2f - 1f
                        samples[index] = normalized * currentAmplitude
                    }
                }
            }

            if (
                track.write(
                    samples,
                    0,
                    samples.size,
                    AudioTrack.WRITE_BLOCKING
                ) < 0
            ) {
                running.set(false)
            } else {
                writtenAmplitude = currentAmplitude
            }
        }
    }

    private fun levelToAmplitude(levelDbfs: Double): Float =
        10.0.pow(levelDbfs / 20.0).toFloat()
}
