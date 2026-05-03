package com.sleepsmart.app.ui.morning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sleepsmart.app.classifier.SleepStage
import com.sleepsmart.app.data.repository.SessionRepository
import com.sleepsmart.app.domain.GetMorningReportUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HypnogramPoint(
    val stage: SleepStage,
    val confidence: Float,
    val startMs: Long
)

data class MorningUiState(
    val sessionId: String = "",
    val dateLabel: String = "",
    val score: Int = 0,
    val timeAsleep: String = "0h 0m",
    val disturbances: Int = 0,
    val epochs: List<HypnogramPoint> = emptyList(),
    val observation: String = "",
    val journal: String = "",
    val journalDirty: Boolean = false
)

@HiltViewModel
class MorningReportViewModel @Inject constructor(
    private val useCase: GetMorningReportUseCase,
    private val sessions: SessionRepository
) : ViewModel() {
    private val _state = MutableStateFlow(MorningUiState())
    val state: StateFlow<MorningUiState> = _state.asStateFlow()

    fun load(sessionId: String) {
        viewModelScope.launch {
            val r = useCase(sessionId) ?: return@launch
            val existing = sessions.byId(sessionId)
            _state.value = MorningUiState(
                sessionId = sessionId,
                dateLabel = r.dateLabel,
                score = r.score,
                timeAsleep = r.timeAsleepLabel,
                disturbances = r.disturbances,
                epochs = r.epochs.map { HypnogramPoint(it.stage, it.confidence, it.startMs) },
                observation = r.observation,
                journal = existing?.journal.orEmpty()
            )
        }
    }

    fun setJournalDraft(text: String) {
        _state.value = _state.value.copy(journal = text, journalDirty = true)
    }

    fun saveJournal() {
        val sid = _state.value.sessionId
        if (sid.isEmpty() || !_state.value.journalDirty) return
        val text = _state.value.journal
        viewModelScope.launch {
            sessions.setJournal(sid, text)
            _state.value = _state.value.copy(journalDirty = false)
        }
    }
}
