package com.sleepsmart.app.core.time

import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** Injectable clock. Use this everywhere instead of Instant.now()/System.currentTimeMillis(). */
interface Clock {
    fun now(): Instant
    fun nowMillis(): Long = now().toEpochMilliseconds()
}

@Singleton
class SystemClock @Inject constructor() : Clock {
    override fun now(): Instant = kotlinx.datetime.Clock.System.now()
}
