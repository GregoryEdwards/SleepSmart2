package com.sleepsmart.app.domain

import com.sleepsmart.app.classifier.SleepStage
import com.sleepsmart.app.data.db.entity.EpochEntity
import javax.inject.Inject
import kotlin.math.roundToInt

class ComputeSleepScoreUseCase @Inject constructor() {

    /**
     * Score in 0..100. Composite of:
     *  - duration  (vs 8h goal, max 40 pts)
     *  - efficiency (asleep / total, max 30 pts)
     *  - depth      (% N3, max 15 pts; target 13–23%)
     *  - rem        (% REM, max 15 pts; target 20–25%)
     */
    fun compute(epochs: List<EpochEntity>, sleepGoalHours: Float = 8f): Score {
        if (epochs.isEmpty()) return Score(0, 0, 0)
        val total = epochs.size
        val asleepEpochs = epochs.count { it.stage != SleepStage.AWAKE.name }
        val n3 = epochs.count { it.stage == SleepStage.N3.name }
        val rem = epochs.count { it.stage == SleepStage.REM.name }

        val asleepHours = asleepEpochs * 0.5f / 60f * 60f / 60f // epochs are 30 s
        val asleepMin = (asleepEpochs * 0.5f).toInt()           // 30 s per epoch -> 0.5 min per epoch
        val totalMin = (total * 0.5f).toInt()

        val durationPts = ((asleepMin / 60f) / sleepGoalHours).coerceIn(0f, 1f) * 40f
        val efficiency = if (total > 0) asleepEpochs.toFloat() / total else 0f
        val efficiencyPts = efficiency.coerceIn(0f, 1f) * 30f
        val n3Frac = if (asleepEpochs > 0) n3.toFloat() / asleepEpochs else 0f
        val remFrac = if (asleepEpochs > 0) rem.toFloat() / asleepEpochs else 0f

        // Tent functions: peak at center of healthy band, taper to 0 at edges.
        val depthPts = tent(n3Frac, lo = 0.05f, peak = 0.18f, hi = 0.30f) * 15f
        val remPts = tent(remFrac, lo = 0.10f, peak = 0.22f, hi = 0.35f) * 15f

        val score = (durationPts + efficiencyPts + depthPts + remPts).roundToInt().coerceIn(0, 100)
        return Score(score = score, asleepMinutes = asleepMin, totalMinutes = totalMin)
    }

    private fun tent(x: Float, lo: Float, peak: Float, hi: Float): Float {
        if (x <= lo || x >= hi) return 0f
        return if (x < peak) (x - lo) / (peak - lo) else (hi - x) / (hi - peak)
    }

    data class Score(val score: Int, val asleepMinutes: Int, val totalMinutes: Int)
}
