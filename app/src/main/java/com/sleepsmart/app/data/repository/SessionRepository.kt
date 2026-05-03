package com.sleepsmart.app.data.repository

import com.sleepsmart.app.classifier.SleepStage
import com.sleepsmart.app.data.db.DisturbanceDao
import com.sleepsmart.app.data.db.EpochDao
import com.sleepsmart.app.data.db.SessionDao
import com.sleepsmart.app.data.db.entity.DisturbanceEntity
import com.sleepsmart.app.data.db.entity.EpochEntity
import com.sleepsmart.app.data.db.entity.SessionEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepository @Inject constructor(
    private val sessionDao: SessionDao,
    private val epochDao: EpochDao,
    private val disturbanceDao: DisturbanceDao
) {
    private val _lastFinishedSessionId = MutableStateFlow<String?>(null)
    val lastFinishedSessionId: StateFlow<String?> = _lastFinishedSessionId

    suspend fun startNew(
        startedAt: Instant,
        targetWakeAt: Instant,
        wakeWindowMinutes: Int
    ): SessionEntity {
        val session = SessionEntity(
            id = UUID.randomUUID().toString(),
            startedAt = startedAt.toEpochMilliseconds(),
            endedAt = null,
            targetWakeAt = targetWakeAt.toEpochMilliseconds(),
            wakeWindowMinutes = wakeWindowMinutes,
            firedAt = null,
            score = null,
            timeAsleepMinutes = null,
            disturbances = 0
        )
        sessionDao.insert(session)
        return session
    }

    suspend fun finishSession(
        sessionId: String,
        endedAt: Instant,
        firedAt: Instant?,
        score: Int,
        timeAsleepMinutes: Int,
        disturbances: Int
    ) {
        val current = sessionDao.byId(sessionId) ?: return
        sessionDao.update(
            current.copy(
                endedAt = endedAt.toEpochMilliseconds(),
                firedAt = firedAt?.toEpochMilliseconds(),
                score = score,
                timeAsleepMinutes = timeAsleepMinutes,
                disturbances = disturbances
            )
        )
        _lastFinishedSessionId.value = sessionId
    }

    suspend fun saveEpochs(epochs: List<EpochEntity>) {
        if (epochs.isEmpty()) return
        epochDao.insertAll(epochs)
    }

    suspend fun saveDisturbances(items: List<DisturbanceEntity>) {
        if (items.isEmpty()) return
        disturbanceDao.insertAll(items)
    }

    suspend fun byId(id: String): SessionEntity? = sessionDao.byId(id)

    suspend fun epochsFor(sid: String): List<EpochEntity> = epochDao.forSession(sid)

    suspend fun disturbanceCountFor(sid: String): Int = disturbanceDao.countForSession(sid)

    fun clearLastFinished() { _lastFinishedSessionId.value = null }

    /** Build an EpochEntity from runtime values. */
    fun newEpoch(
        sessionId: String,
        index: Int,
        startMs: Long,
        durationMs: Long,
        stage: SleepStage,
        confidence: Float,
        rms: Float,
        movementIndex: Float,
        breathRate: Float?,
        breathVariability: Float?
    ) = EpochEntity(
        sessionId = sessionId,
        index = index,
        startMs = startMs,
        durationMs = durationMs,
        stage = stage.name,
        confidence = confidence,
        rms = rms,
        movementIndex = movementIndex,
        breathRate = breathRate,
        breathVariability = breathVariability
    )
}
