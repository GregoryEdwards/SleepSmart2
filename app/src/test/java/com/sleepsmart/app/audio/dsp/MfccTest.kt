package com.sleepsmart.app.audio.dsp

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.PI
import kotlin.math.sin

class MfccTest {

    @Test
    fun `1 kHz sine produces strongly negative MFCC1`() {
        val sr = 8000
        val n = 1024
        val frame = FloatArray(n) { sin(2.0 * PI * 1000.0 * it / sr).toFloat() }
        val hann = Windowing.hann(n)
        for (i in frame.indices) frame[i] *= hann[i]
        val power = Fft(n).powerSpectrum(frame)
        val mfcc = Mfcc(sampleRate = sr, fftSize = n).fromPowerSpectrum(power)
        // For a pure 1 kHz tone the energy is concentrated in mid mel bins,
        // so DCT coefficient 1 (which represents the slope of the log mel
        // spectrum from low to high) should be strongly negative.
        // Tolerance: more negative than -1.0 (well below zero with margin).
        assertTrue(mfcc[1] < -1.0f, "Expected MFCC[1] < -1 for 1 kHz sine, got ${mfcc[1]}")
    }

    @Test
    fun `silence produces low magnitude coefficients`() {
        val n = 1024
        val frame = FloatArray(n) // all zeros
        val power = Fft(n).powerSpectrum(frame)
        val mfcc = Mfcc(fftSize = n).fromPowerSpectrum(power)
        // All ln(0) → ln(1e-12) → -27.6 (approx); since all bands are
        // identical, DCT coefficients beyond the 0th should be ~0.
        for (i in 1 until 13) {
            assertTrue(kotlin.math.abs(mfcc[i]) < 1e-3f,
                "Silence MFCC[$i] should be ~0, got ${mfcc[i]}")
        }
    }
}
