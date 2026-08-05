package com.breadler.boxingperformancetracker.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.breadler.boxingperformancetracker.data.ProcessingPhase
import com.breadler.boxingperformancetracker.data.SessionProcessingState
import com.breadler.boxingperformancetracker.data.SessionRepository
import com.breadler.boxingperformancetracker.data.SessionSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.ArrayDeque
import java.util.Date
import java.util.Locale

class StrykoViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SessionRepository(application.applicationContext)

    val sessions: StateFlow<List<SessionSummary>> = repository.sessions.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    private val _importState = MutableStateFlow(SessionProcessingState())
    val importState: StateFlow<SessionProcessingState> = _importState.asStateFlow()

    /** Videos recorded/imported while another one is still processing wait here, since only
     * one on-device analysis pipeline runs at a time. See [acknowledgeCompletion]. */
    private val pendingVideos = ArrayDeque<QueuedVideo>()

    private val _queuedSessionNames = MutableStateFlow<List<String>>(emptyList())
    val queuedSessionNames: StateFlow<List<String>> = _queuedSessionNames.asStateFlow()

    /** Recording setup carried across screens/navigation so it doesn't reset every time the
     * New Session screen is (re)entered. */
    private val _prepareSeconds = MutableStateFlow(30)
    val prepareSeconds: StateFlow<Int> = _prepareSeconds.asStateFlow()

    private val _workSeconds = MutableStateFlow(180)
    val workSeconds: StateFlow<Int> = _workSeconds.asStateFlow()

    private val _useFrontCamera = MutableStateFlow(false)
    val useFrontCamera: StateFlow<Boolean> = _useFrontCamera.asStateFlow()

    fun setPrepareSeconds(seconds: Int) {
        _prepareSeconds.value = seconds
    }

    fun setWorkSeconds(seconds: Int) {
        _workSeconds.value = seconds
    }

    fun setUseFrontCamera(useFrontCamera: Boolean) {
        _useFrontCamera.value = useFrontCamera
    }

    fun importVideo(videoUri: Uri) {
        val sessionName = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
        if (_importState.value.isProcessing) {
            pendingVideos.addLast(QueuedVideo(videoUri, sessionName))
            _queuedSessionNames.value = pendingVideos.map { it.sessionName }
            return
        }
        beginImport(videoUri, sessionName)
    }

    private fun beginImport(videoUri: Uri, sessionName: String) {
        _importState.value = SessionProcessingState(
            isProcessing = true,
            sessionName = sessionName,
            phase = ProcessingPhase.COPYING,
            progress = 0f,
            startTimeMs = System.currentTimeMillis(),
        )
        viewModelScope.launch {
            repository.importVideo(videoUri, sessionName) { phase, fraction ->
                _importState.value = _importState.value.copy(phase = phase, progress = fraction)
            }
                .onSuccess { session ->
                    _importState.value = _importState.value.copy(
                        isProcessing = false,
                        phase = null,
                        progress = 1f,
                        completedSessionId = session.id,
                    )
                }
                .onFailure { error ->
                    _importState.value = _importState.value.copy(
                        isProcessing = false,
                        phase = null,
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

    /** Dismisses the finished/errored state shown on the status bar and, if a video was queued
     * behind it, starts that one. */
    fun acknowledgeCompletion() {
        _importState.value = SessionProcessingState()
        val next = pendingVideos.poll()
        _queuedSessionNames.value = pendingVideos.map { it.sessionName }
        next?.let { beginImport(it.uri, it.sessionName) }
    }

    private data class QueuedVideo(val uri: Uri, val sessionName: String)

    companion object {
        fun factory(application: Application): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(StrykoViewModel::class.java)) {
                        return StrykoViewModel(application) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }
    }
}
