package com.sleepsmart.app.classifier

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HeuristicS4MTest {

    private fun fv(
        rms: Float = 0.05f,
        zcr: Float = 0.1f,
        centroid: Float = 200f,
        rolloff: Float = 1000f,
        bandEnergies: FloatArray = FloatArray(5),
        mfcc: FloatArray = FloatArray(13),
        breath: Float? = 13f,
        breathVar: Float? = 0.05f,
        movement: Float = 0.05f,
        rmsMedian: Float = 0.05f
    ) = FeatureVector(
        rms = rms, zcr = zcr, spectralCentroid = centroid, rolloff85 = rolloff,
        bandEnergies = bandEnergies, mfcc = mfcc,
        breathRate = breath, breathVariability = breathVar,
        movementIndex = movement, epochDurationMs = 30_000,
        rmsRollingMedian = rmsMedian
    )

    @Test
    fun `loud burst classifies as awake with high confidence`() {
        val c = HeuristicS4M()
        // Prime rolling median with low values
        repeat(15) { c.classify(fv(), epochIndex = it, sessionMinutes = it / 2) }
        val burst = fv(rms = 0.5f, centroid = 1800f, movement = 0.7f)
        val r = c.classify(burst, epochIndex = 16, sessionMinutes = 8)
        assertEquals(SleepStage.AWAKE, r.stage)
        assertTrue(r.confidence >= 0.7f, "expected confidence ≥0.7, got ${r.confidence}")
    }

    @Test
    fun `slow regular breath with no movement classifies as N3`() {
        val c = HeuristicS4M()
        val features = fv(movement = 0.02f, breath = 11f, breathVar = 0.05f)
        val r = c.classify(features, epochIndex = 30, sessionMinutes = 96) // night progress 0.2
        assertEquals(SleepStage.N3, r.stage)
    }

    @Test
    fun `late night irregular breath low movement classifies as REM`() {
        val c = HeuristicS4M()
        val features = fv(movement = 0.04f, breath = 16f, breathVar = 0.4f)
        val r = c.classify(features, epochIndex = 600, sessionMinutes = 408) // night progress 0.85
        assertEquals(SleepStage.REM, r.stage)
    }

    @Test
    fun `light sleep with onset bias produces N1`() {
        val c = HeuristicS4M()
        val features = fv(movement = 0.15f)
        val r = c.classify(features, epochIndex = 3, sessionMinutes = 2)
        assertEquals(SleepStage.N1, r.stage)
    }
}
