package com.sleepsmart.app.audio.dsp

import org.jtransforms.fft.FloatFFT_1D

/**
 * Thin wrapper over JTransforms real FFT.
 *
 * Input is real, length must be a power of two. Returns complex spectrum
 * in JTransforms' "realForward" packed form, but exposes a [powerSpectrum]
 * helper that returns the magnitude-squared at each frequency bin (length n/2 + 1).
 */
class Fft(val n: Int) {
    init {
        require(n > 0 && (n and (n - 1)) == 0) { "FFT size must be power of two: $n" }
    }

    private val impl = FloatFFT_1D(n.toLong())

    /** In-place real forward FFT. After this, [data] holds packed complex output. */
    fun realForwardInPlace(data: FloatArray) {
        require(data.size >= n)
        impl.realForward(data)
    }

    /**
     * Compute the one-sided power spectrum (length n/2 + 1) of a real input
     * of length [n]. Uses the JTransforms packed format:
     *  data[0]   = re(0)
     *  data[1]   = re(n/2)        (Nyquist)
     *  data[2k]  = re(k)          for 1 ≤ k < n/2
     *  data[2k+1]= im(k)          for 1 ≤ k < n/2
     */
    fun powerSpectrum(realInput: FloatArray): FloatArray {
        require(realInput.size == n)
        val data = realInput.copyOf(n)
        impl.realForward(data)
        val half = n / 2
        val out = FloatArray(half + 1)
        out[0] = data[0] * data[0]
        out[half] = data[1] * data[1]
        for (k in 1 until half) {
            val re = data[2 * k]
            val im = data[2 * k + 1]
            out[k] = re * re + im * im
        }
        return out
    }
}
