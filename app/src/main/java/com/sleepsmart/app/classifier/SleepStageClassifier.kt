package com.sleepsmart.app.classifier

interface SleepStageClassifier {
    fun classify(
        features: FeatureVector,
        epochIndex: Int,
        sessionMinutes: Int
    ): StageEstimate

    /** Reset session-scoped state (rolling stats etc.). */
    fun reset()
}
