package com.sleepsmart.app.wake

import com.sleepsmart.app.classifier.SleepStage
import com.sleepsmart.app.classifier.StageEstimate
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes

class SmartWakeEngineTest {

    private val target = Instant.fromEpochMilliseconds(1_700_000_000_000L)

    private fun engine(window: Int = 30) = SmartWakeEngine().apply { configure(target, window) }

    @Test
    fun `fires within window on first qualifying N1 epoch`() {
        val e = engine()
        val now = target.minus(22.minutes)
        val r = e.observe(StageEstimate(SleepStage.N1, 0.6f), now)
        assertNotNull(r)
        assertEquals(now, r)
    }

    @Test
    fun `does not fire before window opens`() {
        val e = engine(window = 30)
        val r = e.observe(StageEstimate(SleepStage.N1, 0.7f), target.minus(50.minutes))
        assertNull(r)
    }

    @Test
    fun `returns null on N3 in early window then fires at fallback`() {
        val e = engine()
        repeat(10) { i ->
            // Spaced > 4 minute cooldown so each is considered
            val now = target.minus((30 - i * 3).minutes)
            assertNull(e.observe(StageEstimate(SleepStage.N3, 0.7f), now))
        }
        assertEquals(target, e.fallbackFire())
    }

    @Test
    fun `cooldown skips second epoch within 4 minutes`() {
        val e = engine()
        val first = e.observe(StageEstimate(SleepStage.N2, 0.6f), target.minus(20.minutes))
        // N2 score = 0.4 + urgency at -20 min in 30 min window = 0.33 → 0.73 < 1
        assertNull(first)
        val secondTooSoon = e.observe(StageEstimate(SleepStage.N1, 0.6f), target.minus(19.minutes))
        // Even though N1 + urgency would qualify, cooldown rejects.
        assertNull(secondTooSoon)
    }

    @Test
    fun `low confidence is rejected`() {
        val e = engine()
        val r = e.observe(StageEstimate(SleepStage.N1, 0.3f), target.minus(15.minutes))
        assertNull(r)
    }

    @Test
    fun `urgency lifts REM into firing late in window`() {
        val e = engine()
        // First call: REM at -25 min — score 0.7 + urgency 0.17 = 0.87 < 1, no fire
        val early = e.observe(StageEstimate(SleepStage.REM, 0.6f), target.minus(25.minutes))
        assertNull(early)
        // Wait > 4 min cooldown, then REM at -3 min — urgency 0.9, score 0.7 + 0.9 = 1.6 ≥ 1
        val late = e.observe(StageEstimate(SleepStage.REM, 0.6f), target.minus(3.minutes))
        assertNotNull(late)
    }

    @Test
    fun `decision is sticky`() {
        val e = engine()
        val first = e.observe(StageEstimate(SleepStage.AWAKE, 0.9f), target.minus(20.minutes))
        assertNotNull(first)
        // Subsequent observations return null even though the engine still has a decision.
        val again = e.observe(StageEstimate(SleepStage.N1, 0.9f), target.minus(15.minutes))
        assertNull(again)
    }
}
