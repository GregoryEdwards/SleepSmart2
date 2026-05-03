package com.sleepsmart.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sleepsmart.app.data.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val wakeWindowMinutes: Int = 30,
    val sleepGoalHours: Float = 8f,
    val demoModeEnabled: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: PreferencesRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            prefs.preferences.collect {
                _state.value = SettingsUiState(
                    wakeWindowMinutes = it.wakeWindowMinutes,
                    sleepGoalHours = it.sleepGoalHours,
                    demoModeEnabled = it.demoModeEnabled
                )
            }
        }
    }

    fun setWakeWindow(minutes: Int) {
        viewModelScope.launch {
            prefs.update { it.copy(wakeWindowMinutes = minutes.coerceIn(15, 45)) }
        }
    }

    fun setSleepGoal(hours: Float) {
        viewModelScope.launch {
            prefs.update { it.copy(sleepGoalHours = hours.coerceIn(6f, 10f)) }
        }
    }

    fun setDemoMode(enabled: Boolean) {
        viewModelScope.launch {
            prefs.update { it.copy(demoModeEnabled = enabled) }
        }
    }
}
