package ru.landilf.dactune.domain.engine

/** Hysteresis prevents the signal from oscillating near a single threshold. */
class AdaptiveSignalPolicy(
    private val muteThresholdDbfs: Double = -42.0,
    private val restoreThresholdDbfs: Double = -48.0
) {
    private var muted = false

    init {
        require(restoreThresholdDbfs < muteThresholdDbfs)
    }

    fun shouldMute(mediaDbfs: Double): Boolean {
        if (mediaDbfs >= muteThresholdDbfs) muted = true
        else if (mediaDbfs <= restoreThresholdDbfs) muted = false
        return muted
    }
}
