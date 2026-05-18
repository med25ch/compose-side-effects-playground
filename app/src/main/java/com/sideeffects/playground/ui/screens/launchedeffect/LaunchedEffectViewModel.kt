package com.sideeffects.playground.ui.screens.launchedeffect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sideeffects.playground.domain.model.LoadState
import com.sideeffects.playground.util.currentTime
import com.sideeffects.playground.util.simulateNetworkCall
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LaunchedEffectUiState(
    val userId: Int = 1,
    val loadState: LoadState<String> = LoadState.Idle,
    val timerSeconds: Int = 0,
    val isTimerRunning: Boolean = false,
    val logs: List<String> = emptyList(),
    val badDemoCounter: Int = 0
)

@HiltViewModel
class LaunchedEffectViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(LaunchedEffectUiState())
    val uiState: StateFlow<LaunchedEffectUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    fun changeUser(newId: Int) {
        addLog("User ID changed to $newId — LaunchedEffect will restart")
        _uiState.update { it.copy(userId = newId, loadState = LoadState.Loading) }
    }

    fun loadUserData(userId: Int) {
        viewModelScope.launch {
            addLog("⟳ Loading user $userId...")
            _uiState.update { it.copy(loadState = LoadState.Loading) }
            val result = simulateNetworkCall(1500)
            result.fold(
                onSuccess = { data ->
                    addLog("✓ Loaded: $data")
                    _uiState.update { it.copy(loadState = LoadState.Success("User $userId: $data")) }
                },
                onFailure = { error ->
                    addLog("✗ Error: ${error.message}")
                    _uiState.update { it.copy(loadState = LoadState.Error(error.message ?: "Unknown")) }
                }
            )
        }
    }

    fun startTimer() {
        if (_uiState.value.isTimerRunning) return
        addLog("Timer started")
        _uiState.update { it.copy(isTimerRunning = true) }
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _uiState.update { it.copy(timerSeconds = it.timerSeconds + 1) }
            }
        }
    }

    fun stopTimer() {
        timerJob?.cancel()
        addLog("Timer stopped at ${_uiState.value.timerSeconds}s")
        _uiState.update { it.copy(isTimerRunning = false) }
    }

    fun resetTimer() {
        timerJob?.cancel()
        _uiState.update { it.copy(timerSeconds = 0, isTimerRunning = false) }
        addLog("Timer reset")
    }

    fun triggerBadDemoRecomposition() {
        _uiState.update { it.copy(badDemoCounter = it.badDemoCounter + 1) }
        addLog("⟳ Recomposition triggered — bad demo will re-launch effect!")
    }

    fun clearLogs() {
        _uiState.update { it.copy(logs = emptyList()) }
    }

    private fun addLog(msg: String) {
        val entry = "[${currentTime()}] $msg"
        _uiState.update { it.copy(logs = it.logs + entry) }
    }
}
