package com.sleepsmart.app.tracking

import com.sleepsmart.app.audio.AudioCapture
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Demo replacement for AudioCapture that produces deterministic samples
 * shaped to drive the classifier through a believable hypnogram in 8 minutes
 * (60× speed: each "epoch" is 0.5 s of synthetic samples representing 30 s
 * of real time).
 *
 * Used only when demo mode is enabled in Settings.
 */
@Singleton
class SyntheticAudioCapture @Inject constructor() : AudioCapture {

    override fun samples(): Flow<ShortArray> = flow {
        val sr = 8000
        val frame = ShortArray(sr / 2) // 0.5 s = 4000 samples; emit at 60× speed
        val rng = Random(42)
        var t = 0.0
        var elapsedSec = 0
        while (true) {
            val (ampScale, breath, movement, voiceLike) = phaseProfile(elapsedSec)
            for (i in frame.indices) {
                val breathOsc = sin(2 * PI * breath * t).toFloat()
                val voice = if (voiceLike) (sin(2 * PI * 1100 * t).toFloat() * 0.4f) else 0f
                val noise = (rng.nextFloat() - 0.5f) * 0.6f
                val burst = if (movement && rng.nextFloat() < 0.02f) (rng.nextFloat() - 0.5f) * 4f else 0f
                val s = (breathOsc * 0.4f + noise + voice + burst) * ampScale
                frame[i] = (s * 32767f).toInt().coerceIn(-32767, 32767).toShort()
                t += 1.0 / sr
            }
            emit(frame.copyOf())
            // 60× speed: 30 s of real audio in 0.5 s of synthetic
            delay(500)
            elapsedSec += 30
        }
    }

    /** Returns (amplitude, breathHz, movementBursts, voiceLike) for elapsed seconds of "real" night. */
    private fun phaseProfile(elapsedSec: Int): PhaseProfile {
        // 8 hours = 28800 s
        return when {
            elapsedSec < 5 * 60 -> PhaseProfile(0.7f, 0.30f, true, true)        // wake
            elapsedSec < 20 * 60 -> PhaseProfile(0.3f, 0.25f, false, false)     // N1/N2
            elapsedSec < 90 * 60 -> PhaseProfile(0.15f, 0.18f, false, false)    // N3 deep
            elapsedSec < 110 * 60 -> PhaseProfile(0.25f, 0.30f, false, false)   // REM
            elapsedSec < 180 * 60 -> PhaseProfile(0.2f, 0.20f, false, false)    // N2
            elapsedSec < 220 * 60 -> PhaseProfile(0.30f, 0.32f, false, false)   // REM
            elapsedSec < 320 * 60 -> PhaseProfile(0.20f, 0.22f, false, false)   // N2
            elapsedSec < 380 * 60 -> PhaseProfile(0.32f, 0.34f, false, false)   // REM
            elapsedSec < 460 * 60 -> PhaseProfile(0.20f, 0.24f, false, false)   // N1/N2
            else -> PhaseProfile(0.4f, 0.30f, true, true)                       // wake
        }
    }

    private data class PhaseProfile(
        val amplitude: Float,
        val breathHz: Float,
        val movementBursts: Boolean,
        val voiceLike: Boolean
    ) {
        operator fun component1() = amplitude
        operator fun component2() = breathHz
        operator fun component3() = movementBursts
        operator fun component4() = voiceLike
    }
}
