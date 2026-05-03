package com.sleepsmart.app.audio.dsp

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.PI
import kotlin.math.sin

class BreathingRateTest {

    @Test
    fun `synthetic envelope at 14 bpm yields bpm in 13 to 15 range`() {
        // Envelope sample rate = 16 Hz, duration = 30 s -> 480 samples
        val esr = 16f
        val durSec = 30
        val n = (esr * durSec).toInt()
        val bpm = 14.0
        val freqHz = bpm / 60.0
        val env = FloatArray(n) {
            // Slowly varying breath envelope; non-negative
            val v = 0.5f + 0.4f * sin(2.0 * PI * freqHz * it / esr).toFloat()
            v
        }
        val r = BreathingRate.fromEnvelope(env, esr)
        assertNotNull(r.bpm, "bpm should not be null for clean periodic envelope")
        val b = r.bpm!!
        assertTrue(b in 13f..15f, "expected 13..15 bpm, got $b")
    }

    @Test
    fun `random envelope returns null bpm`() {
        val rng = java.util.Random(7)
        val n = 320
        val env = FloatArray(n) { (rng.nextFloat() - 0.5f) * 0.01f }
        val r = BreathingRate.fromEnvelope(env, 16f, confidenceThreshold = 0.5f)
        assertNull(r.bpm)
    }
}
