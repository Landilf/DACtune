package ru.landilf.dactune.domain.engine

import ru.landilf.dactune.domain.model.SignalLevelSample

/** A bounded, time-based window; each tick is retained even when the signal level is unchanged. */
class SignalLevelHistory {
    private val samples = ArrayDeque<SignalLevelSample>()

    fun record(elapsedMs: Long, levelDbfs: Double): List<SignalLevelSample> {
        require(levelDbfs.isFinite())
        require(samples.lastOrNull()?.elapsedMs?.let { elapsedMs >= it } != false)
        samples.addLast(SignalLevelSample(elapsedMs, levelDbfs))
        val earliest = elapsedMs - WINDOW_MS
        while (samples.first().elapsedMs < earliest || samples.size > MAX_SAMPLES) {
            samples.removeFirst()
        }
        return samples.toList()
    }

    fun clear() {
        samples.clear()
    }

    companion object {
        const val WINDOW_MS = 30_000L
        private const val MAX_SAMPLES = 61
    }
}
