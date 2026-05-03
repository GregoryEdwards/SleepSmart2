package com.sleepsmart.app.domain

import com.sleepsmart.app.data.db.entity.SessionEntity
import com.sleepsmart.app.data.prefs.UserPreferences
import com.sleepsmart.app.data.repository.PreferencesRepository
import com.sleepsmart.app.data.repository.SessionRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class GetTrendsUseCaseTest {

    private fun session(score: Int?, asleep: Int?, ts: Long, journal: String? = null) =
        SessionEntity(
            id = "s$ts",
            startedAt = ts,
            endedAt = ts + 1,
            targetWakeAt = ts + 1,
            wakeWindowMinutes = 30,
            firedAt = null,
            score = score,
            timeAsleepMinutes = asleep,
            disturbances = 0,
            journal = journal
        )

    private fun useCase(
        sessions: List<SessionEntity>,
        goalHours: Float = 8f
    ): GetTrendsUseCase {
        val repo = mockk<SessionRepository>()
        coEvery { repo.recentFinished(any()) } returns sessions
        val prefs = mockk<PreferencesRepository>()
        every { prefs.preferences } returns flowOf(UserPreferences(sleepGoalHours = goalHours))
        return GetTrendsUseCase(repo, prefs)
    }

    @Test
    fun `empty trends are empty`() = runTest {
        val r = useCase(emptyList()).invoke()
        assertEquals(0, r.sleepDebtMinutes)
        assertNull(r.averageScore)
        assertEquals(emptyList<GetTrendsUseCase.TrendsSession>(), r.sessions)
    }

    @Test
    fun `sleep debt counts only undersleep`() = runTest {
        val sessions = listOf(
            session(score = 80, asleep = 360, ts = 1L),  // -120 min vs 8h goal
            session(score = 70, asleep = 480, ts = 2L),  // 0 (matches goal)
            session(score = 90, asleep = 540, ts = 3L)   // 0 (over the goal)
        )
        val r = useCase(sessions).invoke()
        assertEquals(120, r.sleepDebtMinutes)
        assertEquals(80, r.averageScore)
    }

    @Test
    fun `average score skips null scores`() = runTest {
        val sessions = listOf(
            session(score = null, asleep = 480, ts = 1L),
            session(score = 60, asleep = 480, ts = 2L),
            session(score = 80, asleep = 480, ts = 3L)
        )
        val r = useCase(sessions).invoke()
        assertEquals(70, r.averageScore)
    }
}
