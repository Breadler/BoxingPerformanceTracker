package com.breadler.boxingperformancetracker.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.breadler.boxingperformancetracker.data.SessionProcessingState
import com.breadler.boxingperformancetracker.data.SessionRepository
import com.breadler.boxingperformancetracker.data.SessionSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class StrykoViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SessionRepository(application.applicationContext)

    val sessions: StateFlow<List<SessionSummary>> = repository.sessions.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    private val _importState = MutableStateFlow(SessionProcessingState())
    val importState: StateFlow<SessionProcessingState> = _importState.asStateFlow()

    fun importVideo(videoUri: Uri) {
        viewModelScope.launch {
            _importState.value = SessionProcessingState(
                isProcessing = true,
                statusMessage = "Preparing video...",
                progress = 0f,
            )
            repository.importVideo(videoUri) { status, fraction ->
                _importState.value = _importState.value.copy(
                    isProcessing = true,
                    statusMessage = status,
                    progress = fraction,
                )
            }
                .onSuccess { session ->
                    _importState.value = SessionProcessingState(
                        isProcessing = false,
                        statusMessage = "Import finished.",
                        progress = 1f,
                        completedSessionId = session.id,
                    )
                }
                .onFailure { error ->
                    _importState.value = SessionProcessingState(
                        isProcessing = false,
                        statusMessage = "Import failed.",
                        errorMessage = error.message ?: "Unknown processing error.",
                    )
                }
        }
    }

    suspend fun getSession(sessionId: String): SessionSummary? = repository.getSession(sessionId)

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
        }
    }

    fun clearImportState() {
        _importState.value = SessionProcessingState()
    }

    companion object {
        fun factory(application: Application): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(StrykoViewModel::class.java)) {
                        return StrykoViewModel(application) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${'$'}{modelClass.name}")
                }
            }
        }
    }
}
