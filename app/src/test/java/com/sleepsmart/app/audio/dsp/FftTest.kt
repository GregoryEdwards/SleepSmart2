package com.sleepsmart.app.audio.dsp

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.PI
import kotlin.math.sin

class FftTest {

    @Test
    fun `power spectrum length is n_over_2 plus 1`() {
        val fft = Fft(1024)
        val ps = fft.powerSpectrum(FloatArray(1024))
        assertEquals(513, ps.size)
    }

    @Test
    fun `power spectrum peaks at the input frequency bin`() {
        val n = 1024
        val sr = 8000
        val freq = 1000
        val frame = FloatArray(n) { sin(2.0 * PI * freq * it / sr).toFloat() }
        val ps = Fft(n).powerSpectrum(frame)
        // Expected bin: freq / (sr/n) = 1000 / 7.8125 = 128
        val peakBin = ps.indices.maxBy { ps[it] }
        assertTrue(peakBin in 125..132, "peak bin out of range: $peakBin")
    }
}
