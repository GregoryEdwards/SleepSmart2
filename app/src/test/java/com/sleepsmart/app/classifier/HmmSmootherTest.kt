package com.sleepsmart.app.classifier

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HmmSmootherTest {

    @Test
    fun `replaces 1-epoch spike with modal stage`() {
        val s = HmmSmoother()
        val seq = listOf(
            StageEstimate(SleepStage.N2, 0.6f),
            StageEstimate(SleepStage.N2, 0.6f),
            StageEstimate(SleepStage.REM, 0.6f),
            StageEstimate(SleepStage.N2, 0.6f),
            StageEstimate(SleepStage.N2, 0.6f)
        )
        val out = seq.map { s.process(it) }
        assertEquals(SleepStage.N2, out[2].stage,
            "spike should be replaced; got ${out[2].stage}")
        assertTrue(out[2].confidence < 0.6f,
            "confidence should be reduced after replacement")
    }

    @Test
    fun `stable run is preserved`() {
        val s = HmmSmoother()
        val seq = List(8) { StageEstimate(SleepStage.N2, 0.7f) }
        val out = seq.map { s.process(it) }
        assertTrue(out.all { it.stage == SleepStage.N2 })
    }
}
