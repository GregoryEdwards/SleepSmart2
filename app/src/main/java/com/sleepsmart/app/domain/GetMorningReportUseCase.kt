package com.sleepsmart.app.domain

import com.sleepsmart.app.classifier.SleepStage
import com.sleepsmart.app.data.repository.PreferencesRepository
import com.sleepsmart.app.data.repository.SessionRepository
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class GetMorningReportUseCase @Inject constructor(
    private val sessions: SessionRepository,
    private val prefs: PreferencesRepository,
    private val score: ComputeSleepScoreUseCase
) {
    data class HypnogramEpoch(val stage: SleepStage, val confidence: Float, val startMs: Long)

    data class MorningReport(
        val dateLabel: String,
        val score: Int,
        val timeAsleepLabel: String,
        val disturbances: Int,
        val epochs: List<HypnogramEpoch>,
        val observation: String
    )

    suspend operator fun invoke(sessionId: String): MorningReport? {
        val session = sessions.byId(sessionId) ?: return null
        val rows = sessions.epochsFor(sessionId)
        val disturbances = sessions.disturbanceCountFor(sessionId)
        val pref = prefs.preferences.first()
        val s = score.compute(rows, pref.sleepGoalHours)

        val date = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
            .format(Date(session.startedAt))
        val asleepLabel = "%dh %02dm".format(s.asleepMinutes / 60, s.asleepMinutes % 60)
        val obs = generateObservation(rows, s, disturbances)

        val hyp = rows.map {
            HypnogramEpoch(
                stage = SleepStage.valueOf(it.stage),
                confidence = it.confidence,
                startMs = it.startMs
            )
        }
        return MorningReport(
            dateLabel = date,
            score = s.score,
            timeAsleepLabel = asleepLabel,
            disturbances = disturbances,
            epochs = hyp,
            observation = obs
        )
    }

    private fun generateObservation(
        rows: List<com.sleepsmart.app.data.db.entity.EpochEntity>,
        s: ComputeSleepScoreUseCase.Score,
        disturbances: Int
    ): String {
        if (rows.isEmpty()) return "Nothing to report yet."
        val firstSleep = rows.indexOfFirst { it.stage != SleepStage.AWAKE.name }
        val onsetMin = (firstSleep * 0.5f).toInt()
        val n3 = rows.count { it.stage == SleepStage.N3.name }
        val n3Min = (n3 * 0.5f).toInt()
        val rem = rows.count { it.stage == SleepStage.REM.name }
        val remMin = (rem * 0.5f).toInt()

        return when {
            disturbances >= 5 -> "Several disturbances tonight — your room may have been louder than usual."
            onsetMin in 1..10 -> "You fell asleep in $onsetMin minutes — easier than most nights."
            n3Min >= 60 -> "Plenty of deep sleep ($n3Min min) — good for memory and recovery."
            remMin >= 90 -> "A long REM stretch ($remMin min) — your dreaming brain was busy."
            s.score >= 80 -> "A clean night. Score ${s.score}."
            s.score < 50 -> "Lighter than usual. Tomorrow night could use an earlier wind-down."
            else -> "An ordinary night. ${s.asleepMinutes / 60}h ${s.asleepMinutes % 60}m asleep."
        }
    }
}
