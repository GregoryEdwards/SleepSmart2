package com.sleepsmart.app.domain

import com.sleepsmart.app.classifier.SleepStage
import com.sleepsmart.app.data.db.entity.EpochEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ComputeSleepScoreUseCaseTest {

    private fun epoch(stage: SleepStage, idx: Int) = EpochEntity(
        sessionId = "s",
        index = idx,
        startMs = idx * 30_000L,
        durationMs = 30_000L,
        stage = stage.name,
        confidence = 0.7f,
        rms = 0.05f,
        movementIndex = 0.05f,
        breathRate = 14f,
        breathVariability = 0.1f
    )

    @Test
    fun `empty returns zero score`() {
        val r = ComputeSleepScoreUseCase().compute(emptyList())
        assertEquals(0, r.score)
        assertEquals(0, r.asleepMinutes)
    }

    @Test
    fun `healthy night yields high score`() {
        // 8 h of mostly N2 with reasonable N3 and REM, minimal awake
        val total = 960
        val list = ArrayList<EpochEntity>(total)
        var i = 0
        repeat(20) { list.add(epoch(SleepStage.AWAKE, i++)) }       // 10 min awake (sleep latency)
        repeat(180) { list.add(epoch(SleepStage.N3, i++)) }         // 90 min N3 (~19% of asleep)
        repeat(220) { list.add(epoch(SleepStage.REM, i++)) }        // 110 min REM (~23% of asleep)
        repeat(540) { list.add(epoch(SleepStage.N2, i++)) }         // 270 min N2
        val r = ComputeSleepScoreUseCase().compute(list)
        assertTrue(r.score in 70..100, "expected high score, got ${r.score}")
        assertTrue(r.asleepMinutes >= 470)
    }

    @Test
    fun `mostly awake yields low score`() {
        val list = (0 until 480).map { epoch(SleepStage.AWAKE, it) }
        val r = ComputeSleepScoreUseCase().compute(list)
        assertTrue(r.score < 30, "expected low score, got ${r.score}")
    }
}
