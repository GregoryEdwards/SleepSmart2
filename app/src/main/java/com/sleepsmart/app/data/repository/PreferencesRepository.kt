package com.sleepsmart.app.data.repository

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sleepsmart.app.data.prefs.UserPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore("user_prefs")

@Singleton
class PreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private object Keys {
        val TARGET = intPreferencesKey("target_wake_minutes")
        val WINDOW = intPreferencesKey("wake_window_minutes")
        val GOAL = floatPreferencesKey("sleep_goal_hours")
        val ONBOARDED = booleanPreferencesKey("onboarding_complete")
        val PERMS = booleanPreferencesKey("permissions_acked")
        val DEMO = booleanPreferencesKey("demo_mode_enabled")
    }

    val preferences: Flow<UserPreferences> = context.dataStore.data.map { it.toModel() }

    suspend fun snapshot(): UserPreferences {
        var result: UserPreferences = UserPreferences()
        context.dataStore.data.collect { result = it.toModel(); return@collect }
        return result
    }

    suspend fun update(transform: (UserPreferences) -> UserPreferences) {
        context.dataStore.edit { prefs ->
            val current = prefs.toModel()
            val next = transform(current)
            prefs[Keys.TARGET] = next.targetWakeAtMinutesOfDay
            prefs[Keys.WINDOW] = next.wakeWindowMinutes
            prefs[Keys.GOAL] = next.sleepGoalHours
            prefs[Keys.ONBOARDED] = next.onboardingComplete
            prefs[Keys.PERMS] = next.permissionsAcknowledged
            prefs[Keys.DEMO] = next.demoModeEnabled
        }
    }

    private fun Preferences.toModel(): UserPreferences {
        val defaults = UserPreferences()
        return UserPreferences(
            targetWakeAtMinutesOfDay = this[Keys.TARGET] ?: defaults.targetWakeAtMinutesOfDay,
            wakeWindowMinutes = this[Keys.WINDOW] ?: defaults.wakeWindowMinutes,
            sleepGoalHours = this[Keys.GOAL] ?: defaults.sleepGoalHours,
            onboardingComplete = this[Keys.ONBOARDED] ?: defaults.onboardingComplete,
            permissionsAcknowledged = this[Keys.PERMS] ?: defaults.permissionsAcknowledged,
            demoModeEnabled = this[Keys.DEMO] ?: defaults.demoModeEnabled
        )
    }
}
