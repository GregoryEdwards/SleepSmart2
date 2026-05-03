package com.sleepsmart.app.audio.dsp

import kotlin.math.PI
import kotlin.math.cos

object Windowing {
    fun hann(n: Int): FloatArray {
        require(n > 0)
        val w = FloatArray(n)
        if (n == 1) {
            w[0] = 1f
            return w
        }
        val denom = (n - 1).toDouble()
        for (i in 0 until n) {
            w[i] = (0.5 * (1.0 - cos(2.0 * PI * i / denom))).toFloat()
        }
        return w
    }
}
