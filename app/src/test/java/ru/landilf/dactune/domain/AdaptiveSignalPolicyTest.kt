package ru.landilf.dactune.domain

import org.junit.Assert.*
import org.junit.Test
import ru.landilf.dactune.domain.engine.AdaptiveSignalPolicy
import ru.landilf.dactune.domain.engine.SignalLevelAnalyzer
import ru.landilf.dactune.domain.model.KeepAliveSettings

class AdaptiveSignalPolicyTest {
    @Test fun levelContinuouslyDecreasesAsMediaGetsLouder() {
        val policy = AdaptiveSignalPolicy()
        assertEquals(-40.0, policy.targetLevelDbfs(-48.0, -40.0, -80.0), 0.001)
        assertEquals(-43.0, policy.targetLevelDbfs(-45.0, -40.0, -80.0), 0.001)
        assertEquals(-50.0, policy.targetLevelDbfs(-38.0, -40.0, -80.0), 0.001)
        assertEquals(-60.0, policy.targetLevelDbfs(-28.0, -40.0, -80.0), 0.001)
        assertEquals(-58.5, policy.targetLevelDbfs(-29.5, -40.0, -80.0), 0.001)
    }

    @Test fun silenceRestoresKeepAliveAfterLoudMedia() {
        val policy = AdaptiveSignalPolicy()
        assertEquals(-78.0, policy.targetLevelDbfs(-10.0, -40.0, -80.0), 0.001)
        assertEquals(-40.0, policy.targetLevelDbfs(
            SignalLevelAnalyzer().rmsDbfs(FloatArray(100), 100), -40.0, -80.0), 0.001)
    }

    @Test fun reductionIsBoundedAndAlwaysFiniteForSupportedSettings() {
        val policy = AdaptiveSignalPolicy()
        for (selected in listOf(-60.0, -40.0, -20.0)) {
            assertEquals(selected - 40.0, policy.targetLevelDbfs(0.0, selected, selected - 40.0), 0.001)
            assertEquals(selected - 40.0, policy.targetLevelDbfs(Double.POSITIVE_INFINITY, selected, selected - 40.0), 0.001)
            assertEquals(selected, policy.targetLevelDbfs(Double.NEGATIVE_INFINITY, selected, selected - 40.0), 0.001)
            assertEquals(selected, policy.targetLevelDbfs(Double.NaN, selected, selected - 40.0), 0.001)
        }
    }

    @Test fun customLowerBoundaryLimitsAttenuation() {
        val policy = AdaptiveSignalPolicy()
        assertEquals(-55.0, policy.targetLevelDbfs(-10.0, -40.0, -55.0), 0.001)
        assertEquals(-40.0, policy.targetLevelDbfs(-100.0, -40.0, -55.0), 0.001)
        assertEquals(-50.0, policy.targetLevelDbfs(-38.0, -40.0, -55.0), 0.001)
        assertEquals(-40.0, policy.targetLevelDbfs(0.0, -40.0, -40.0), 0.001)
    }

    @Test fun boundariesAreQuantizedAndCannotCross() {
        assertEquals(-80.0, KeepAliveSettings().lowerLevelDbfs, 0.0)
        assertEquals(-100.0, KeepAliveSettings(levelDbfs = -60.0).lowerLevelDbfs, 0.0)
        assertEquals(-100.0, KeepAliveSettings(lowerLevelDbfs = -200.0).normalized().lowerLevelDbfs, 0.0)
        assertEquals(-80.5, KeepAliveSettings(lowerLevelDbfs = -80.6).normalized().lowerLevelDbfs, 0.0)
        assertEquals(-40.0, KeepAliveSettings(lowerLevelDbfs = -30.0).normalized().lowerLevelDbfs, 0.0)
        assertEquals(-60.0, KeepAliveSettings(levelDbfs = -60.0,
            lowerLevelDbfs = -50.0).normalized().lowerLevelDbfs, 0.0)
    }

    @Test fun legacyDisabledTimerMigratesToUntilAppClosed() {
        val untilClosed = KeepAliveSettings(timerMinutes = KeepAliveSettings.UNTIL_APP_CLOSED).normalized()
        assertTrue(untilClosed.untilAppClosed)
        assertTrue(KeepAliveSettings(timerMinutes = 0).normalized().untilAppClosed)
        assertTrue(KeepAliveSettings().untilAppClosed)
        assertFalse(0 in KeepAliveSettings.TIMER_OPTIONS)
        assertFalse(KeepAliveSettings(timerMinutes = 15).untilAppClosed)
        assertTrue(KeepAliveSettings.UNTIL_APP_CLOSED in KeepAliveSettings.TIMER_OPTIONS)
    }

    @Test fun analyzerUsesOnlyReadSamples() {
        val samples = floatArrayOf(0.1f, -0.1f, 1f)
        assertEquals(-20.0, SignalLevelAnalyzer().rmsDbfs(samples, 2), 0.001)
    }

    @Test fun storedSettingsAreClampedAndQuantized() {
        assertEquals(-20.0, KeepAliveSettings(levelDbfs = 10.0).normalized().levelDbfs, 0.0)
        assertEquals(-60.0, KeepAliveSettings(levelDbfs = -100.0).normalized().levelDbfs, 0.0)
        assertEquals(-40.0, KeepAliveSettings(levelDbfs = Double.NaN).normalized().levelDbfs, 0.0)
        assertEquals(-40.5, KeepAliveSettings(levelDbfs = -40.6).normalized().levelDbfs, 0.0)
        assertEquals(KeepAliveSettings.UNTIL_APP_CLOSED,
            KeepAliveSettings(timerMinutes = 42).normalized().timerMinutes)
    }
}
