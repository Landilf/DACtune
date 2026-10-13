package ru.landilf.dactune.domain.model

import kotlin.math.roundToInt

data class KeepAliveSettings(
    val signal: KeepAliveSignal = KeepAliveSignal.TONE_18_KHZ,
    val levelDbfs: Double = -40.0,
    val timerMinutes: Int = 0,
    val adaptiveEnabled: Boolean = false
) {
    fun normalized() = copy(
        levelDbfs = levelFromProgress(progressFromLevel(levelDbfs)),
        timerMinutes = timerMinutes.takeIf { it in TIMER_OPTIONS } ?: 0
    )

    companion object {
        const val MIN_LEVEL_DBFS = -60.0
        const val MAX_LEVEL_DBFS = -20.0
        const val LEVEL_STEP_DB = 0.5
        const val LEVEL_STEP_COUNT = 80
        val TIMER_OPTIONS = listOf(0, 15, 30, 45, 60, 90, 120)

        fun progressFromLevel(level: Double): Int =
            ((level.takeIf { it.isFinite() } ?: -40.0)
                .coerceIn(MIN_LEVEL_DBFS, MAX_LEVEL_DBFS)
                .minus(MIN_LEVEL_DBFS) / LEVEL_STEP_DB).roundToInt()

        fun levelFromProgress(progress: Int): Double =
            MIN_LEVEL_DBFS + progress.coerceIn(0, LEVEL_STEP_COUNT) * LEVEL_STEP_DB
    }
}
