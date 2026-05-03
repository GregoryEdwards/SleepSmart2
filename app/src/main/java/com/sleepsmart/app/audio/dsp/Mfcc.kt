package com.sleepsmart.app.audio.dsp

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max

/**
 * Standard MFCC pipeline:
 *  pre-emphasis → frame → window → FFT → mel filterbank → log → DCT-II → first 13 coefs.
 *
 * In SleepSmart we feed in already-windowed power spectrum frames and then
 * compute mean MFCC over the frames in the 30 s epoch.
 */
class Mfcc(
    private val sampleRate: Int = 8000,
    private val fftSize: Int = 1024,
    private val melFilters: Int = 26,
    private val coefficients: Int = 13,
    private val lowFreq: Float = 0f,
    private val highFreq: Float = 4000f
) {
    private val filterBank: Array<FloatArray> = buildMelFilterBank()
    private val dctMatrix: Array<FloatArray> = buildDctMatrix()

    /** Compute MFCCs from a precomputed power spectrum. Returns 13 coefficients. */
    fun fromPowerSpectrum(power: FloatArray): FloatArray {
        require(power.size == fftSize / 2 + 1)
        val melEnergies = FloatArray(melFilters)
        for (m in 0 until melFilters) {
            var s = 0.0
            val filter = filterBank[m]
            for (k in power.indices) s += filter[k] * power[k]
            melEnergies[m] = ln(max(1e-12, s)).toFloat()
        }
        val out = FloatArray(coefficients)
        for (c in 0 until coefficients) {
            var s = 0.0
            for (m in 0 until melFilters) s += dctMatrix[c][m] * melEnergies[m]
            out[c] = s.toFloat()
        }
        return out
    }

    private fun hzToMel(hz: Float): Float = (1127.0 * ln(1.0 + hz / 700.0)).toFloat()
    private fun melToHz(m: Float): Float = (700.0 * (exp(m / 1127.0) - 1.0)).toFloat()

    private fun buildMelFilterBank(): Array<FloatArray> {
        val melLow = hzToMel(lowFreq)
        val melHigh = hzToMel(highFreq)
        val melPoints = FloatArray(melFilters + 2) {
            melLow + (melHigh - melLow) * it / (melFilters + 1)
        }
        val hzPoints = FloatArray(melPoints.size) { melToHz(melPoints[it]) }
        val binCount = fftSize / 2 + 1
        val binHz = sampleRate.toDouble() / fftSize
        val bins = IntArray(hzPoints.size) { (hzPoints[it] / binHz).toInt().coerceIn(0, binCount - 1) }
        return Array(melFilters) { m ->
            val f = FloatArray(binCount)
            val l = bins[m]
            val c = bins[m + 1]
            val r = bins[m + 2]
            if (c > l) for (k in l..c) f[k] = (k - l).toFloat() / (c - l)
            if (r > c) for (k in c..r) f[k] = (r - k).toFloat() / (r - c)
            f
        }
    }

    private fun buildDctMatrix(): Array<FloatArray> {
        // DCT-II, orthonormal-ish (sufficient for MFCCs)
        val out = Array(coefficients) { FloatArray(melFilters) }
        for (c in 0 until coefficients) {
            for (m in 0 until melFilters) {
                out[c][m] = cos(PI * c * (m + 0.5) / melFilters).toFloat()
            }
        }
        return out
    }

    /**
     * Convenience: full pipeline from a real-valued window. Applies pre-emphasis,
     * Hann-windows in place, runs FFT (caller-provided), and returns MFCCs.
     */
    fun fromFrame(frame: FloatArray, fft: Fft, hann: FloatArray): FloatArray {
        require(frame.size == fftSize)
        val pre = preEmphasis(frame)
        for (i in pre.indices) pre[i] *= hann[i]
        val power = fft.powerSpectrum(pre)
        return fromPowerSpectrum(power)
    }

    private fun preEmphasis(input: FloatArray, alpha: Float = 0.97f): FloatArray {
        val out = FloatArray(input.size)
        out[0] = input[0]
        for (i in 1 until input.size) {
            out[i] = input[i] - alpha * input[i - 1]
        }
        return out
    }
}
