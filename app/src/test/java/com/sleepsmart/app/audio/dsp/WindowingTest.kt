package com.sleepsmart.app.audio.dsp

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WindowingTest {
    @Test
    fun `hann endpoints are zero`() {
        val w = Windowing.hann(64)
        assertEquals(0f, w.first(), 1e-6f)
        assertEquals(0f, w.last(), 1e-6f)
    }

    @Test
    fun `hann peaks at center`() {
        val w = Windowing.hann(65)
        val peak = w.max()
        assertTrue(peak >= 0.99f)
        assertTrue(w[32] >= peak - 1e-3f)
    }
}
