package com.sleepsmart.app.ui.tonight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sleepsmart.app.data.prefs.UserPreferences
import com.sleepsmart.app.data.repository.PreferencesRepository
import com.sleepsmart.app.data.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TonightUiState(
    val targetWakeMinutesOfDay: Int = 7 * 60,
    val wakeWindowMinutes: Int = 30,
    val sleepGoalHours: Float = 8f,
    val lastFinishedSessionId: String? = null
) {
    val formattedTargetWakeTime: String
        get() {
            val h24 = targetWakeMinutesOfDay / 60
            val m = targetWakeMinutesOfDay % 60
            val h12 = if (h24 == 0) 12 else if (h24 > 12) h24 - 12 else h24
            val ampm = if (h24 < 12) "am" else "pm"
            return "%d:%02d %s".format(h12, m, ampm)
        }
}

@HiltViewModel
class TonightViewModel @Inject constructor(
    private val prefs: PreferencesRepository,
    private val sessions: SessionRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TonightUiState())
    val state: StateFlow<TonightUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(prefs.preferences, sessions.lastFinishedSessionId) { p, sid ->
                TonightUiState(
                    targetWakeMinutesOfDay = p.targetWakeAtMinutesOfDay,
                    wakeWindowMinutes = p.wakeWindowMinutes,
                    sleepGoalHours = p.sleepGoalHours,
                    lastFinishedSessionId = sid
                )
            }.collect { _state.value = it }
        }
    }

    fun setWakeWindow(minutes: Int) {
        viewModelScope.launch {
            prefs.update { it.copy(wakeWindowMinutes = minutes.coerceIn(15, 45)) }
        }
    }

    fun consumeLastFinished() {
        viewModelScope.launch { sessions.clearLastFinished() }
    }
}
