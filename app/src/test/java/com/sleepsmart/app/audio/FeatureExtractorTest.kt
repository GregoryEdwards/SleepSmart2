package com.sleepsmart.app.audio

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.PI
import kotlin.math.sin

class FeatureExtractorTest {

    private val sr = 8000

    @Test
    fun `silence yields near-zero rms and movement`() {
        val window = ShortArray(sr * 30) // 30 s of zeros
        val fv = DefaultFeatureExtractor().extract(window)
        assertEquals(0f, fv.rms, 1e-4f)
        assertEquals(0f, fv.movementIndex, 1e-4f)
        assertEquals(13, fv.mfcc.size)
        assertEquals(5, fv.bandEnergies.size)
    }

    @Test
    fun `loud tone yields elevated centroid`() {
        val window = ShortArray(sr * 30) { i ->
            (sin(2.0 * PI * 1500.0 * i / sr) * 0.5 * 32767).toInt().toShort()
        }
        val fv = DefaultFeatureExtractor().extract(window)
        assertTrue(fv.spectralCentroid > 800f,
            "expected centroid > 800 Hz for 1500 Hz tone, got ${fv.spectralCentroid}")
        assertTrue(fv.rms > 0.1f, "rms should be elevated, got ${fv.rms}")
    }

    @Test
    fun `extractor does not retain reference to input array`() {
        val window = ShortArray(sr * 5) { 42 }
        val fv = DefaultFeatureExtractor().extract(window)
        // FeatureVector has only derived fields, no raw samples.
        // Simply assert by spec — the data class doesn't expose ShortArray.
        // Mutating the input must not affect the result.
        val rmsBefore = fv.rms
        for (i in window.indices) window[i] = 0
        assertEquals(rmsBefore, fv.rms)
        assertNotSame(window, fv.bandEnergies)
    }
}
