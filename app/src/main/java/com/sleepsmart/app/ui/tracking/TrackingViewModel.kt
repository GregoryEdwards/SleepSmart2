package com.sleepsmart.app.ui.tracking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sleepsmart.app.core.time.Clock
import com.sleepsmart.app.data.repository.SessionRepository
import com.sleepsmart.app.tracking.SessionState
import com.sleepsmart.app.tracking.TrackingStateHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TrackingUiState(
    val clockText: String = "--:--",
    val epochsCaptured: Int = 0,
    val sessionStartedAtMs: Long? = null,
    val finishedSessionId: String? = null
) {
    val formattedDuration: String
        get() {
            val start = sessionStartedAtMs ?: return "00:00"
            val elapsed = (System.currentTimeMillis() - start) / 1000
            val h = elapsed / 3600
            val m = (elapsed % 3600) / 60
            return if (h > 0) "%d:%02d".format(h, m) else "0:%02d".format(m)
        }
}

@HiltViewModel
class TrackingViewModel @Inject constructor(
    private val trackingState: TrackingStateHolder,
    private val sessions: SessionRepository,
    private val clock: Clock
) : ViewModel() {

    private val _state = MutableStateFlow(TrackingUiState())
    val state: StateFlow<TrackingUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            while (true) {
                val s = trackingState.state.value
                val now = clock.nowMillis()
                val clockStr = formatClock(now)
                _state.value = when (s) {
                    is SessionState.Tracking -> TrackingUiState(
                        clockText = clockStr,
                        epochsCaptured = s.epochsCaptured,
                        sessionStartedAtMs = s.startedAtMs
                    )
                    is SessionState.Finished -> TrackingUiState(
                        clockText = clockStr,
                        finishedSessionId = s.sessionId
                    )
                    SessionState.Idle -> TrackingUiState(clockText = clockStr)
                }
                delay(1000)
            }
        }
    }

    private fun formatClock(ms: Long): String {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = ms }
        val h24 = cal.get(java.util.Calendar.HOUR_OF_DAY)
        val m = cal.get(java.util.Calendar.MINUTE)
        val h12 = if (h24 == 0) 12 else if (h24 > 12) h24 - 12 else h24
        return "%d:%02d".format(h12, m)
    }
}
