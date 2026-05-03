package com.sleepsmart.app.ui.trends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sleepsmart.app.domain.GetTrendsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TrendsUiState(
    val sessions: List<GetTrendsUseCase.TrendsSession> = emptyList(),
    val averageScore: Int? = null,
    val sleepDebtMinutes: Int = 0,
    val goalHours: Float = 8f,
    val loading: Boolean = true
)

@HiltViewModel
class TrendsViewModel @Inject constructor(
    private val getTrends: GetTrendsUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(TrendsUiState())
    val state: StateFlow<TrendsUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            val t = getTrends()
            _state.value = TrendsUiState(
                sessions = t.sessions,
                averageScore = t.averageScore,
                sleepDebtMinutes = t.sleepDebtMinutes,
                goalHours = t.goalHours,
                loading = false
            )
        }
    }
}
