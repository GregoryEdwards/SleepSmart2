package com.sleepsmart.app.audio

import com.sleepsmart.app.audio.dsp.BreathingRate
import com.sleepsmart.app.audio.dsp.Fft
import com.sleepsmart.app.audio.dsp.Mfcc
import com.sleepsmart.app.audio.dsp.SpectralFeatures
import com.sleepsmart.app.audio.dsp.Windowing
import com.sleepsmart.app.classifier.FeatureVector
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.sqrt

interface FeatureExtractor {
    fun extract(window: ShortArray): FeatureVector
}

@Singleton
class DefaultFeatureExtractor @Inject constructor() : FeatureExtractor {

    private val sampleRate = AndroidAudioCapture.SAMPLE_RATE
    private val fftSize = 1024
    private val hopSize = 512                  // 50% overlap
    private val fft = Fft(fftSize)
    private val hann = Windowing.hann(fftSize)
    private val mfccCalc = Mfcc(sampleRate = sampleRate, fftSize = fftSize)

    private var rmsRollingMedian = 0f
    private val rmsHistory = ArrayDeque<Float>()
    private val rmsHistoryMax = 120 // ~30 epochs of inputs ≈ rolling median for movement gating

    override fun extract(window: ShortArray): FeatureVector {
        if (window.isEmpty()) return empty()

        val samples = FloatArray(window.size)
        var rms = 0.0
        var zeroCrossings = 0
        var prev: Short = window[0]
        for (i in window.indices) {
            val v = window[i]
            samples[i] = v / 32768f
            rms += (v.toDouble() * v.toDouble())
            if ((prev >= 0) != (v >= 0)) zeroCrossings++
            prev = v
        }
        rms = sqrt(rms / window.size) / 32768.0
        val rmsF = rms.toFloat()

        // Roll the median estimate (use mean of last N as a cheap proxy).
        rmsHistory.addLast(rmsF)
        while (rmsHistory.size > rmsHistoryMax) rmsHistory.removeFirst()
        rmsRollingMedian = rmsHistory.toFloatArray().median()

        val zcr = zeroCrossings.toFloat() / window.size

        // Frame loop
        val frameCount = ((window.size - fftSize) / hopSize).coerceAtLeast(0) + 1
        val avgPower = FloatArray(fftSize / 2 + 1)
        val mfccAccum = FloatArray(13)
        val envelope = FloatArray(frameCount.coerceAtLeast(1))

        // Movement: count of frames whose broadband energy exceeds 6 dB above
        // the median of frame energies in this window.
        val frameEnergies = FloatArray(frameCount.coerceAtLeast(1))

        if (frameCount > 0 && window.size >= fftSize) {
            for (f in 0 until frameCount) {
                val start = f * hopSize
                if (start + fftSize > window.size) break
                val frame = FloatArray(fftSize)
                System.arraycopy(samples, start, frame, 0, fftSize)
                // pre-emphasis + hann
                var prevS = 0f
                for (i in frame.indices) {
                    val raw = frame[i]
                    val pe = raw - 0.97f * prevS
                    prevS = raw
                    frame[i] = pe * hann[i]
                }
                val power = fft.powerSpectrum(frame)
                for (k in power.indices) avgPower[k] += power[k]

                val mfcc = mfccCalc.fromPowerSpectrum(power)
                for (i in 0 until 13) mfccAccum[i] += mfcc[i]

                var frameSqSum = 0f
                for (i in start until start + fftSize) frameSqSum += samples[i] * samples[i]
                val frameRms = sqrt(frameSqSum / fftSize)
                envelope[f] = frameRms
                frameEnergies[f] = frameSqSum
            }
            for (k in avgPower.indices) avgPower[k] /= frameCount
            for (i in 0 until 13) mfccAccum[i] /= frameCount
        }

        val centroid = SpectralFeatures.centroid(avgPower, sampleRate)
        val rolloff = SpectralFeatures.rolloff(avgPower, sampleRate)
        val bandEnergies = SpectralFeatures.bandEnergies(avgPower, sampleRate)

        val envelopeSampleRate = sampleRate.toFloat() / hopSize
        val breath = BreathingRate.fromEnvelope(envelope, envelopeSampleRate)

        val movementIndex = movementIndex(frameEnergies)

        return FeatureVector(
            rms = rmsF,
            zcr = zcr,
            spectralCentroid = centroid,
            rolloff85 = rolloff,
            bandEnergies = bandEnergies,
            mfcc = mfccAccum,
            breathRate = breath.bpm,
            breathVariability = breath.variability,
            movementIndex = movementIndex,
            epochDurationMs = (window.size * 1000L) / sampleRate,
            rmsRollingMedian = rmsRollingMedian
        )
    }

    private fun movementIndex(frameEnergies: FloatArray): Float {
        if (frameEnergies.isEmpty()) return 0f
        val median = frameEnergies.copyOf().median()
        if (median <= 0f) return 0f
        // 6 dB above median → 4× linear
        val threshold = median * 4f
        var hits = 0
        for (e in frameEnergies) if (e > threshold) hits++
        return hits.toFloat() / frameEnergies.size
    }

    private fun empty(): FeatureVector = FeatureVector(
        rms = 0f, zcr = 0f, spectralCentroid = 0f, rolloff85 = 0f,
        bandEnergies = FloatArray(5), mfcc = FloatArray(13),
        breathRate = null, breathVariability = null,
        movementIndex = 0f, epochDurationMs = 0L, rmsRollingMedian = 0f
    )
}

private fun FloatArray.median(): Float {
    if (isEmpty()) return 0f
    val sorted = copyOf().also { it.sort() }
    val n = sorted.size
    return if (n % 2 == 1) sorted[n / 2]
    else 0.5f * (sorted[n / 2 - 1] + sorted[n / 2])
}
