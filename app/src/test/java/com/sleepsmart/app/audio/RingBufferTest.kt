package com.sleepsmart.app.audio

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RingBufferTest {

    @Test
    fun `partial fill returns chronological prefix`() {
        val rb = RingBuffer(8)
        rb.write(shortArrayOf(1, 2, 3, 4))
        assertArrayEquals(shortArrayOf(1, 2, 3, 4), rb.snapshot())
        assertEquals(4, rb.available)
    }

    @Test
    fun `wrap returns last N samples in order`() {
        val rb = RingBuffer(4)
        // Write 6 samples: [1,2,3,4,5,6]; expect snapshot = [3,4,5,6]
        rb.write(shortArrayOf(1, 2, 3, 4, 5, 6))
        assertEquals(4, rb.available)
        assertArrayEquals(shortArrayOf(3, 4, 5, 6), rb.snapshot())
    }

    @Test
    fun `1_5x capacity wrap correctness`() {
        val rb = RingBuffer(10)
        val data = ShortArray(15) { (it + 1).toShort() }
        rb.write(data)
        // Last 10 samples in order: 6..15
        val expected = ShortArray(10) { (it + 6).toShort() }
        assertArrayEquals(expected, rb.snapshot())
    }

    @Test
    fun `reset zeroes state`() {
        val rb = RingBuffer(4)
        rb.write(shortArrayOf(1, 2, 3))
        rb.reset()
        assertEquals(0, rb.available)
        assertArrayEquals(ShortArray(0), rb.snapshot())
    }
}
