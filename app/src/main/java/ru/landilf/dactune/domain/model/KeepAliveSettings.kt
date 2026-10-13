package ru.landilf.dactune.domain.model

import kotlin.math.roundToInt

data class KeepAliveSettings(
    val signal: KeepAliveSignal = KeepAliveSignal.TONE_18_KHZ,
    val levelDbfs: Double = -40.0,
    val timerMinutes: Int = UNTIL_APP_CLOSED,
    val adaptiveEnabled: Boolean = false,
    val lowerLevelDbfs: Double = levelDbfs - 40.0
) {
    val untilAppClosed: Boolean
        get() = timerMinutes == UNTIL_APP_CLOSED

    fun normalized(): KeepAliveSettings {
        val upper = levelFromProgress(progressFromLevel(levelDbfs))
        val lower = lowerLevelFromProgress(lowerProgressFromLevel(lowerLevelDbfs))
        return copy(
            levelDbfs = upper,
            lowerLevelDbfs = lower.coerceAtMost(upper),
            timerMinutes = timerMinutes.takeIf { it in TIMER_OPTIONS } ?: UNTIL_APP_CLOSED
        )
    }

    companion object {
        const val MIN_LEVEL_DBFS = -60.0
        const val MAX_LEVEL_DBFS = -20.0
        const val LEVEL_STEP_DB = 0.5
        const val LEVEL_STEP_COUNT = 80
        const val MIN_ADAPTIVE_LEVEL_DBFS = -100.0
        const val LOWER_LEVEL_STEP_COUNT = 160
        const val UNTIL_APP_CLOSED = -1
        val TIMER_OPTIONS = listOf(UNTIL_APP_CLOSED, 15, 30, 45, 60, 90, 120)

        fun progressFromLevel(level: Double): Int =
            ((level.takeIf { it.isFinite() } ?: -40.0)
                .coerceIn(MIN_LEVEL_DBFS, MAX_LEVEL_DBFS)
                .minus(MIN_LEVEL_DBFS) / LEVEL_STEP_DB).roundToInt()

        fun levelFromProgress(progress: Int): Double =
            MIN_LEVEL_DBFS + progress.coerceIn(0, LEVEL_STEP_COUNT) * LEVEL_STEP_DB

        fun lowerProgressFromLevel(level: Double): Int =
            (((level.takeIf { it.isFinite() } ?: -80.0)
                .coerceIn(MIN_ADAPTIVE_LEVEL_DBFS, MAX_LEVEL_DBFS) - MIN_ADAPTIVE_LEVEL_DBFS) /
                LEVEL_STEP_DB).roundToInt()

        fun lowerLevelFromProgress(progress: Int): Double =
            MIN_ADAPTIVE_LEVEL_DBFS + progress.coerceIn(0, LOWER_LEVEL_STEP_COUNT) * LEVEL_STEP_DB
    }
}
