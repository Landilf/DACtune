package ru.landilf.dactune.domain

import org.junit.Assert.*
import org.junit.Test
import ru.landilf.dactune.domain.engine.AdaptiveSignalPolicy
import ru.landilf.dactune.domain.engine.SignalLevelAnalyzer
import ru.landilf.dactune.domain.model.KeepAliveSettings

class AdaptiveSignalPolicyTest {
    @Test fun hysteresisRetainsPreviousDecisionBetweenThresholds() {
        val policy = AdaptiveSignalPolicy()
        assertFalse(policy.shouldMute(-45.0))
        assertTrue(policy.shouldMute(-42.0))
        assertTrue(policy.shouldMute(-45.0))
        assertFalse(policy.shouldMute(-48.0))
        assertFalse(policy.shouldMute(-45.0))
    }

    @Test fun silenceRestoresKeepAliveAfterLoudMedia() {
        val policy = AdaptiveSignalPolicy()
        assertTrue(policy.shouldMute(-10.0))
        assertFalse(policy.shouldMute(SignalLevelAnalyzer().rmsDbfs(FloatArray(100), 100)))
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
        assertEquals(0, KeepAliveSettings(timerMinutes = 42).normalized().timerMinutes)
    }
}
