package com.sleepsmart.app.audio.dsp

import kotlin.math.log10
import kotlin.math.max

object SpectralFeatures {

    /** Spectral centroid in Hz, weighted by magnitude (sqrt of power). */
    fun centroid(power: FloatArray, sampleRate: Int): Float {
        var num = 0.0
        var den = 0.0
        val n = (power.size - 1) * 2 // FFT size
        for (k in power.indices) {
            val mag = kotlin.math.sqrt(power[k].toDouble())
            val freq = k * sampleRate.toDouble() / n
            num += freq * mag
            den += mag
        }
        return if (den <= 0.0) 0f else (num / den).toFloat()
    }

    /** Frequency below which [percentile] of spectral energy lies. */
    fun rolloff(power: FloatArray, sampleRate: Int, percentile: Float = 0.85f): Float {
        var total = 0.0
        for (p in power) total += p
        if (total <= 0.0) return 0f
        val target = total * percentile
        var running = 0.0
        val n = (power.size - 1) * 2
        for (k in power.indices) {
            running += power[k]
            if (running >= target) {
                return (k * sampleRate.toDouble() / n).toFloat()
            }
        }
        return (sampleRate / 2).toFloat()
    }

    /**
     * Log10 band energies for the canonical SleepSmart bands (Hz):
     *  [50,125), [125,500), [500,1000), [1000,2000), [2000,4000].
     */
    fun bandEnergies(power: FloatArray, sampleRate: Int): FloatArray {
        val edges = floatArrayOf(50f, 125f, 500f, 1000f, 2000f, 4000f)
        val out = FloatArray(5)
        val n = (power.size - 1) * 2
        val binHz = sampleRate.toDouble() / n
        for (b in 0 until 5) {
            val lo = (edges[b] / binHz).toInt().coerceIn(0, power.size - 1)
            val hi = (edges[b + 1] / binHz).toInt().coerceIn(lo, power.size - 1)
            var sum = 0.0
            for (k in lo..hi) sum += power[k]
            out[b] = log10(max(1e-12, sum)).toFloat()
        }
        return out
    }
}
