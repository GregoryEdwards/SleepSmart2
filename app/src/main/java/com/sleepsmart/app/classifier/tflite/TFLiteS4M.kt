package com.sleepsmart.app.classifier.tflite

import com.sleepsmart.app.classifier.FeatureVector
import com.sleepsmart.app.classifier.SleepStageClassifier
import com.sleepsmart.app.classifier.StageEstimate

/**
 * Stub: the trained S4M model is out of scope for v0.1. Architectural seam
 * only — never wired into Hilt by default.
 */
class TFLiteS4M : SleepStageClassifier {
    override fun classify(
        features: FeatureVector,
        epochIndex: Int,
        sessionMinutes: Int
    ): StageEstimate = throw NotImplementedError(
        "TFLiteS4M is a stub for v1.x — wire HeuristicS4M for v0.1"
    )

    override fun reset() = Unit
}
