package com.sleepsmart.app.data.prefs

data class UserPreferences(
    val targetWakeAtMinutesOfDay: Int = 7 * 60,
    val wakeWindowMinutes: Int = 30,
    val sleepGoalHours: Float = 8f,
    val onboardingComplete: Boolean = false,
    val permissionsAcknowledged: Boolean = false,
    val demoModeEnabled: Boolean = false
)
