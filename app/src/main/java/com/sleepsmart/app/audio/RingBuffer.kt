package com.sleepsmart.app.audio

/**
 * Fixed-size circular short buffer. Caller writes batches of samples and reads
 * a chronological snapshot via [snapshot]. Single-writer / single-reader.
 */
class RingBuffer(capacityBytes: Int) {
    init { require(capacityBytes > 0) { "capacity must be positive" } }

    private val buf = ShortArray(capacityBytes)
    val capacity: Int get() = buf.size

    private var writeIdx = 0
    private var totalWritten: Long = 0L

    /** Number of unique samples currently held (≤ capacity). */
    val available: Int get() = if (totalWritten >= capacity) capacity else totalWritten.toInt()

    fun write(samples: ShortArray, length: Int = samples.size) {
        var i = 0
        while (i < length) {
            buf[writeIdx] = samples[i]
            writeIdx = (writeIdx + 1) % capacity
            i++
        }
        totalWritten += length
    }

    /**
     * Snapshot of the most recent [available] samples in chronological order.
     * Allocates a new ShortArray.
     */
    fun snapshot(): ShortArray {
        val n = available
        val out = ShortArray(n)
        if (n == 0) return out
        if (totalWritten < capacity) {
            // Buffer not yet wrapped; oldest is at 0
            System.arraycopy(buf, 0, out, 0, n)
        } else {
            val start = writeIdx // oldest sample
            val tail = capacity - start
            System.arraycopy(buf, start, out, 0, tail)
            if (start > 0) System.arraycopy(buf, 0, out, tail, start)
        }
        return out
    }

    fun reset() {
        writeIdx = 0
        totalWritten = 0L
        buf.fill(0)
    }
}
