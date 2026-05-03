package com.sleepsmart.app.tracking

import com.sleepsmart.app.classifier.FeatureVector
import com.sleepsmart.app.classifier.StageEstimate
import com.sleepsmart.app.data.db.entity.EpochEntity
import com.sleepsmart.app.data.repository.SessionRepository

/**
 * Buffers epochs in memory and flushes to the repository in small batches.
 * Single-threaded usage (called from the service collector).
 */
class EpochAggregator(
    private val sessionId: String,
    private val repo: SessionRepository,
    private val flushEvery: Int = 8
) {
    private val pending = ArrayList<EpochEntity>(flushEvery)

    fun add(
        index: Int,
        startMs: Long,
        estimate: StageEstimate,
        features: FeatureVector
    ): EpochEntity {
        val e = repo.newEpoch(
            sessionId = sessionId,
            index = index,
            startMs = startMs,
            durationMs = features.epochDurationMs,
            stage = estimate.stage,
            confidence = estimate.confidence,
            rms = features.rms,
            movementIndex = features.movementIndex,
            breathRate = features.breathRate,
            breathVariability = features.breathVariability
        )
        pending.add(e)
        return e
    }

    fun shouldFlush(): Boolean = pending.size >= flushEvery

    suspend fun flush() {
        if (pending.isEmpty()) return
        val copy = ArrayList(pending)
        pending.clear()
        repo.saveEpochs(copy)
    }
}
