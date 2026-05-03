package com.sleepsmart.app.domain

import com.sleepsmart.app.data.repository.PreferencesRepository
import com.sleepsmart.app.data.repository.SessionRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import kotlin.math.max

class GetTrendsUseCase @Inject constructor(
    private val sessions: SessionRepository,
    private val prefs: PreferencesRepository
) {
    data class Trends(
        val sessions: List<TrendsSession>,
        val averageScore: Int?,
        val sleepDebtMinutes: Int,
        val goalHours: Float
    )

    data class TrendsSession(
        val id: String,
        val startedAtMs: Long,
        val score: Int?,
        val asleepMinutes: Int?,
        val journal: String?
    )

    suspend operator fun invoke(limit: Int = 7): Trends {
        val rows = sessions.recentFinished(limit)
        val pref = prefs.preferences.first()
        val goalMin = (pref.sleepGoalHours * 60).toInt()

        val mapped = rows.map {
            TrendsSession(
                id = it.id,
                startedAtMs = it.startedAt,
                score = it.score,
                asleepMinutes = it.timeAsleepMinutes,
                journal = it.journal
            )
        }

        val avg = mapped.mapNotNull { it.score }.takeIf { it.isNotEmpty() }
            ?.average()?.toInt()

        // Sleep debt over the last `limit` nights — sum of (goal - asleep) clipped to 0,
        // i.e. only undersleep counts.
        val debt = mapped.sumOf { s ->
            val asleep = s.asleepMinutes ?: 0
            max(0, goalMin - asleep)
        }

        return Trends(
            sessions = mapped,
            averageScore = avg,
            sleepDebtMinutes = debt,
            goalHours = pref.sleepGoalHours
        )
    }
}
