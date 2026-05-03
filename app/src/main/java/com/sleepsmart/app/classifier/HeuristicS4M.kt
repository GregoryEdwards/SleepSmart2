package com.sleepsmart.app.classifier

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stateful, session-scoped heuristic classifier. Lives for the duration of a
 * single tracking session; reset on session start. See spec §6.2.
 */
@Singleton
class HeuristicS4M @Inject constructor() : SleepStageClassifier {

    private val rmsHistory = ArrayDeque<Float>() // last 30 epochs
    private val rmsHistoryMax = 30
    private var lastStage: SleepStage? = null

    override fun classify(
        features: FeatureVector,
        epochIndex: Int,
        sessionMinutes: Int
    ): StageEstimate {
        val nightProgress = (sessionMinutes / 480f).coerceIn(0f, 1f)
        rmsHistory.addLast(features.rms)
        while (rmsHistory.size > rmsHistoryMax) rmsHistory.removeFirst()
        val rmsMedian = rollingMedian()

        val transitionedFromAwake = lastStage == SleepStage.AWAKE

        val stage: SleepStage
        val confidence: Float

        when {
            features.movementIndex > 0.35f ||
                features.spectralCentroid > 1500f ||
                (rmsMedian > 0f && features.rms > rmsMedian * 4f) -> {
                stage = SleepStage.AWAKE
                confidence = 0.80f
            }
            features.movementIndex < 0.08f -> {
                val br = features.breathRate
                val bv = features.breathVariability
                when {
                    br != null && bv != null && br < 14f && bv < 0.10f -> {
                        stage = SleepStage.N3
                        confidence = (0.65f + 0.15f * (1f - nightProgress)).coerceIn(0f, 1f)
                    }
                    br != null && bv != null && bv > 0.25f && nightProgress > 0.35f -> {
                        stage = SleepStage.REM
                        confidence = (0.55f + 0.20f * nightProgress).coerceIn(0f, 1f)
                    }
                    else -> {
                        stage = SleepStage.N2
                        confidence = 0.55f
                    }
                }
            }
            features.movementIndex < 0.20f -> {
                if (epochIndex < 10 || transitionedFromAwake) {
                    stage = SleepStage.N1
                    confidence = 0.55f
                } else {
                    stage = SleepStage.N2
                    confidence = 0.55f
                }
            }
            else -> {
                stage = SleepStage.N1
                confidence = 0.45f
            }
        }

        lastStage = stage
        return StageEstimate(stage, confidence)
    }

    override fun reset() {
        rmsHistory.clear()
        lastStage = null
    }

    private fun rollingMedian(): Float {
        if (rmsHistory.isEmpty()) return 0f
        val arr = rmsHistory.toFloatArray()
        arr.sort()
        val n = arr.size
        return if (n % 2 == 1) arr[n / 2] else 0.5f * (arr[n / 2 - 1] + arr[n / 2])
    }
}
