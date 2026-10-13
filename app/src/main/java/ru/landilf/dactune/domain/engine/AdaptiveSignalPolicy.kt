package ru.landilf.dactune.domain.engine

/** Continuous attenuation above the quiet-media threshold, while keeping a nonzero signal. */
class AdaptiveSignalPolicy(
    private val quietThresholdDbfs: Double = -48.0
) {
    init {
        require(quietThresholdDbfs.isFinite())
    }

    fun targetLevelDbfs(
        mediaDbfs: Double,
        selectedLevelDbfs: Double,
        lowerLevelDbfs: Double
    ): Double {
        require(selectedLevelDbfs.isFinite())
        require(lowerLevelDbfs.isFinite() && lowerLevelDbfs <= selectedLevelDbfs)
        if (mediaDbfs.isNaN()) return selectedLevelDbfs
        val reduction = (mediaDbfs - quietThresholdDbfs)
            .coerceIn(0.0, selectedLevelDbfs - lowerLevelDbfs)
        return selectedLevelDbfs - reduction
    }
}
