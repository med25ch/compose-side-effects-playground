package com.sideeffects.playground.ui.screens.remembercoroutinescope

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sideeffects.playground.util.currentTime
import com.sideeffects.playground.util.simulateNetworkCall
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RcsUiState(
    val snackbarMessage: String? = null,
    val isUploading: Boolean = false,
    val uploadResult: String? = null,
    val logs: List<String> = emptyList()
)

@HiltViewModel
class RememberCoroutineScopeViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(RcsUiState())
    val uiState: StateFlow<RcsUiState> = _uiState.asStateFlow()

    fun uploadFile() {
        viewModelScope.launch {
            addLog("Upload started")
            _uiState.update { it.copy(isUploading = true, uploadResult = null) }
            val result = simulateNetworkCall(2000)
            result.fold(
                onSuccess = {
                    addLog("✓ Upload succeeded")
                    _uiState.update { s ->
                        s.copy(
                            isUploading = false,
                            uploadResult = "Uploaded at ${currentTime()}",
                            snackbarMessage = "File uploaded successfully!"
                        )
                    }
                },
                onFailure = {
                    addLog("✗ Upload failed: ${it.message}")
                    _uiState.update { s ->
                        s.copy(
                            isUploading = false,
                            snackbarMessage = "Upload failed: ${it.message}"
                        )
                    }
                }
            )
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun clearLogs() {
        _uiState.update { it.copy(logs = emptyList()) }
    }

    private fun addLog(msg: String) {
        _uiState.update { it.copy(logs = it.logs + "[${currentTime()}] $msg") }
    }
}
