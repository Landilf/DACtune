package ru.landilf.dactune.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.landilf.dactune.domain.engine.SignalLevelHistory

class SignalLevelHistoryTest {
    @Test fun constantLevelStillRecordsEveryTickAndKeepsThirtySeconds() {
        val history = SignalLevelHistory()
        var samples = history.record(0, -40.0)
        for (tick in 1..100) {
            samples = history.record(tick * 500L, -40.0)
        }
        assertEquals(61, samples.size)
        assertEquals(20_000L, samples.first().elapsedMs)
        assertEquals(50_000L, samples.last().elapsedMs)
        assertTrue(samples.all { it.levelDbfs == -40.0 })
    }

    @Test fun timeGapRemovesStaleSamplesAndOldSnapshotsStayUnchanged() {
        val history = SignalLevelHistory()
        val firstSnapshot = history.record(0, -40.0)
        val latest = history.record(31_000, -60.0)
        assertEquals(1, latest.size)
        assertEquals(31_000L, latest.single().elapsedMs)
        assertEquals(0L, firstSnapshot.single().elapsedMs)
        assertEquals(-40.0, firstSnapshot.single().levelDbfs, 0.0)
    }

    @Test fun resetDoesNotCarrySamplesIntoNextSession() {
        val history = SignalLevelHistory()
        history.record(0, -40.0)
        history.clear()
        val samples = history.record(500, -60.0)
        assertEquals(1, samples.size)
        assertEquals(-60.0, samples.single().levelDbfs, 0.0)
    }

    @Test fun bufferIsBoundedEvenWhenSamplesArriveFaster() {
        val history = SignalLevelHistory()
        var samples = history.record(0, -40.0)
        for (tick in 1..1000) {
            samples = history.record(tick.toLong(), -40.0)
        }
        assertEquals(61, samples.size)
    }
}
