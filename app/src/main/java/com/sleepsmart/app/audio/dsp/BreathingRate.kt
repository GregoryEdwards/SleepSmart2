package com.sleepsmart.app.audio.dsp

import kotlin.math.sqrt

data class BreathingResult(val bpm: Float?, val variability: Float?)

object BreathingRate {

    /**
     * @param envelope per-frame RMS envelope across the 30 s window
     * @param envelopeSampleRate samples-per-second of the envelope
     * @param confidenceThreshold normalized autocorrelation peak below which
     *        we return null rather than invent a number.
     */
    fun fromEnvelope(
        envelope: FloatArray,
        envelopeSampleRate: Float,
        confidenceThreshold: Float = 0.20f
    ): BreathingResult {
        if (envelope.size < 16) return BreathingResult(null, null)

        val smoothed = movingAverage(envelope, 5)
        val centered = FloatArray(smoothed.size)
        var mean = 0.0
        for (v in smoothed) mean += v
        mean /= smoothed.size
        for (i in smoothed.indices) centered[i] = (smoothed[i] - mean).toFloat()

        val minBpm = 6f
        val maxBpm = 30f
        val maxLag = (60f * envelopeSampleRate / minBpm).toInt().coerceAtMost(centered.size - 1)
        val minLag = (60f * envelopeSampleRate / maxBpm).toInt().coerceAtLeast(1)
        if (maxLag <= minLag) return BreathingResult(null, null)

        val ac = FloatArray(maxLag + 1)
        var zeroLag = 0.0
        for (v in centered) zeroLag += v * v
        if (zeroLag <= 0.0) return BreathingResult(null, null)

        for (lag in minLag..maxLag) {
            var s = 0.0
            for (i in 0 until centered.size - lag) {
                s += centered[i] * centered[i + lag]
            }
            ac[lag] = (s / zeroLag).toFloat()
        }

        // Find peak in the bracket
        var bestLag = -1
        var bestVal = Float.NEGATIVE_INFINITY
        for (lag in minLag..maxLag) {
            if (ac[lag] > bestVal) {
                bestVal = ac[lag]
                bestLag = lag
            }
        }
        if (bestLag < 0 || bestVal < confidenceThreshold) {
            return BreathingResult(null, null)
        }
        val bpm = 60f * envelopeSampleRate / bestLag

        // Variability: std-dev of inter-peak intervals around the dominant period.
        val variability = peakIntervalVariability(centered, bestLag)
        return BreathingResult(bpm = bpm, variability = variability)
    }

    private fun movingAverage(x: FloatArray, k: Int): FloatArray {
        if (k <= 1) return x.copyOf()
        val out = FloatArray(x.size)
        val half = k / 2
        for (i in x.indices) {
            var sum = 0f
            var n = 0
            val lo = (i - half).coerceAtLeast(0)
            val hi = (i + half).coerceAtMost(x.size - 1)
            for (j in lo..hi) {
                sum += x[j]
                n++
            }
            out[i] = sum / n
        }
        return out
    }

    private fun peakIntervalVariability(centered: FloatArray, expectedLag: Int): Float {
        // Find local maxima above 0; take their indices; compute std-dev / mean.
        val peaks = ArrayList<Int>()
        for (i in 1 until centered.size - 1) {
            if (centered[i] > 0 &&
                centered[i] > centered[i - 1] &&
                centered[i] > centered[i + 1]
            ) peaks.add(i)
        }
        if (peaks.size < 3) return 0.5f // fall back to "moderate" variability
        val intervals = ArrayList<Int>()
        for (i in 1 until peaks.size) intervals.add(peaks[i] - peaks[i - 1])
        // Filter intervals close to expectedLag (within 50%)
        val filtered = intervals.filter {
            it >= expectedLag * 0.5 && it <= expectedLag * 1.5
        }
        if (filtered.size < 2) return 0.5f
        val mean = filtered.sum().toDouble() / filtered.size
        if (mean <= 0.0) return 0.5f
        var sq = 0.0
        for (v in filtered) sq += (v - mean) * (v - mean)
        val sd = sqrt(sq / filtered.size)
        return (sd / mean).toFloat()
    }
}
