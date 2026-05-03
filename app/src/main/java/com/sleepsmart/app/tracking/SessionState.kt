package com.sleepsmart.app.tracking

sealed interface SessionState {
    data object Idle : SessionState
    data class Tracking(
        val sessionId: String,
        val startedAtMs: Long,
        val epochsCaptured: Int
    ) : SessionState
    data class Finished(val sessionId: String) : SessionState
}
