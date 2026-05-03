package com.sleepsmart.app.audio.dsp

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.PI
import kotlin.math.sin

class SpectralFeaturesTest {

    private val sr = 8000
    private val n = 1024

    private fun powerOfSine(freqHz: Float): FloatArray {
        val frame = FloatArray(n) { sin(2.0 * PI * freqHz * it / sr).toFloat() }
        val hann = Windowing.hann(n)
        for (i in frame.indices) frame[i] *= hann[i]
        return Fft(n).powerSpectrum(frame)
    }

    @Test
    fun `centroid increases with input frequency`() {
        var prev = -1f
        for (f in listOf(200f, 600f, 1200f, 2400f, 3000f)) {
            val c = SpectralFeatures.centroid(powerOfSine(f), sr)
            assertTrue(c > prev, "centroid not monotonic at $f Hz: prev=$prev, got=$c")
            prev = c
        }
    }

    @Test
    fun `rolloff captures most energy below the rolloff frequency`() {
        val power = powerOfSine(1000f)
        val r = SpectralFeatures.rolloff(power, sr, 0.85f)
        // 1 kHz tone -> rolloff should be near 1 kHz (within ±300 Hz tolerance)
        assertTrue(r in 700f..1300f, "rolloff out of range: $r")
    }

    @Test
    fun `band energies pick correct band for tone`() {
        val tone = powerOfSine(700f)
        val be = SpectralFeatures.bandEnergies(tone, sr)
        // Band [500, 1000) is index 2 — should dominate.
        assertEquals(2, be.indices.maxBy { be[it] })
    }
}
