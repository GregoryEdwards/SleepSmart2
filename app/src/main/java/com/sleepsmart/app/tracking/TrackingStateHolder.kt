package com.sleepsmart.app.tracking

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrackingStateHolder @Inject constructor() {
    private val _state = MutableStateFlow<SessionState>(SessionState.Idle)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    fun set(state: SessionState) { _state.value = state }
    fun setTracking(sessionId: String, startedAtMs: Long, epochs: Int) {
        _state.value = SessionState.Tracking(sessionId, startedAtMs, epochs)
    }
    fun setFinished(sessionId: String) { _state.value = SessionState.Finished(sessionId) }
    fun clear() { _state.value = SessionState.Idle }
}
