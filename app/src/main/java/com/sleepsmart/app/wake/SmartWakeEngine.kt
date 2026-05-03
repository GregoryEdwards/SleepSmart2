package com.sleepsmart.app.wake

import com.sleepsmart.app.classifier.SleepStage
import com.sleepsmart.app.classifier.StageEstimate
import kotlinx.datetime.Instant
import javax.inject.Inject

/**
 * Pure stateful function over a stream of stage estimates. Decides a single
 * `fireAt` Instant within `[targetWakeAt - windowMinutes, targetWakeAt]`.
 */
class SmartWakeEngine @Inject constructor() {

    private var targetWakeAt: Instant? = null
    private var windowMinutes: Int = 30
    private var lastConsideredAt: Instant? = null
    private var decision: Instant? = null

    /** (Re)initialize at the start of a session. */
    fun configure(targetWakeAt: Instant, windowMinutes: Int) {
        this.targetWakeAt = targetWakeAt
        this.windowMinutes = windowMinutes
        this.lastConsideredAt = null
        this.decision = null
    }

    /**
     * Observe a stage estimate at [now]. Returns a non-null Instant only the
     * first time a decision is made (to allow [reschedule]); subsequent calls
     * return null even if the cached decision still stands.
     */
    fun observe(estimate: StageEstimate, now: Instant): Instant? {
        val target = targetWakeAt ?: return null
        if (decision != null) return null

        val minutesToTarget = ((target - now).inWholeSeconds / 60.0)
        if (minutesToTarget > windowMinutes || minutesToTarget < 0) return null
        if (estimate.confidence < 0.55f) return null

        // Cooldown: only consider one candidate every 4 minutes
        val last = lastConsideredAt
        if (last != null && (now - last).inWholeSeconds < 4 * 60) return null
        lastConsideredAt = now

        val urgency = (1.0 - minutesToTarget / windowMinutes).coerceIn(0.0, 1.0).toFloat()
        val score = stageScore(estimate.stage) + urgency
        if (score >= 1.0f) {
            decision = now
            return now
        }
        return null
    }

    /**
     * Force a decision at the target time if the stream ends without an early
     * decision. Always returns the target wake time.
     */
    fun fallbackFire(): Instant? {
        val t = targetWakeAt ?: return null
        if (decision == null) decision = t
        return decision
    }

    /** Visible for testing. */
    internal fun stageScore(s: SleepStage): Float = when (s) {
        SleepStage.AWAKE -> 1.0f
        SleepStage.N1 -> 0.9f
        SleepStage.REM -> 0.7f
        SleepStage.N2 -> 0.4f
        SleepStage.N3 -> 0.0f
    }
}
