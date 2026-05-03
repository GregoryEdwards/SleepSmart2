package com.sleepsmart.app.classifier

enum class SleepStage { AWAKE, N1, N2, N3, REM }

data class StageEstimate(val stage: SleepStage, val confidence: Float)

data class FeatureVector(
    val rms: Float,
    val zcr: Float,
    val spectralCentroid: Float,
    val rolloff85: Float,
    val bandEnergies: FloatArray,        // size 5
    val mfcc: FloatArray,                // size 13
    val breathRate: Float?,
    val breathVariability: Float?,
    val movementIndex: Float,            // 0..1
    val epochDurationMs: Long,
    val rmsRollingMedian: Float = 0f
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FeatureVector) return false
        return rms == other.rms && zcr == other.zcr &&
            spectralCentroid == other.spectralCentroid &&
            rolloff85 == other.rolloff85 &&
            bandEnergies.contentEquals(other.bandEnergies) &&
            mfcc.contentEquals(other.mfcc) &&
            breathRate == other.breathRate &&
            breathVariability == other.breathVariability &&
            movementIndex == other.movementIndex &&
            epochDurationMs == other.epochDurationMs &&
            rmsRollingMedian == other.rmsRollingMedian
    }

    override fun hashCode(): Int {
        var r = rms.hashCode()
        r = 31 * r + zcr.hashCode()
        r = 31 * r + spectralCentroid.hashCode()
        r = 31 * r + rolloff85.hashCode()
        r = 31 * r + bandEnergies.contentHashCode()
        r = 31 * r + mfcc.contentHashCode()
        r = 31 * r + (breathRate?.hashCode() ?: 0)
        r = 31 * r + (breathVariability?.hashCode() ?: 0)
        r = 31 * r + movementIndex.hashCode()
        r = 31 * r + epochDurationMs.hashCode()
        r = 31 * r + rmsRollingMedian.hashCode()
        return r
    }
}
