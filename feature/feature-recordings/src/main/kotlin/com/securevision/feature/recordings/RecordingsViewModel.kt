package com.securevision.feature.recordings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securevision.core.common.result.getOrDefault
import com.securevision.core.domain.usecase.invoke
import com.securevision.core.domain.usecase.recording.DeleteRecordingUseCase
import com.securevision.core.domain.usecase.recording.GetRecordingsUseCase
import com.securevision.core.model.Recording
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Drives the recordings gallery. */
@HiltViewModel
class RecordingsViewModel @Inject constructor(
    getRecordings: GetRecordingsUseCase,
    private val deleteRecording: DeleteRecordingUseCase,
) : ViewModel() {

    private val _pendingDeletion = MutableStateFlow<Recording?>(null)

    /** The clip awaiting delete confirmation, if any. */
    val pendingDeletion: StateFlow<Recording?> = _pendingDeletion.asStateFlow()

    /** Current screen state. */
    val uiState: StateFlow<RecordingsUiState> = getRecordings()
        .map { result ->
            val recordings = result.getOrDefault(emptyList())

            if (recordings.isEmpty()) {
                RecordingsUiState.Empty
            } else {
                RecordingsUiState.Content(recordings)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MILLIS),
            initialValue = RecordingsUiState.Loading,
        )

    /**
     * Asks for confirmation before removing a clip.
     *
     * Confirmed rather than undoable: the file is deleted from disk, and holding a
     * copy so it could be restored would defeat the point of freeing the space.
     *
     * @param recording The clip the user tapped delete on.
     */
    fun onDeleteRequested(recording: Recording) {
        _pendingDeletion.value = recording
    }

    /** Abandons a pending deletion. */
    fun onDeleteCancelled() {
        _pendingDeletion.value = null
    }

    /** Deletes the clip awaiting confirmation, file and row together. */
    fun onDeleteConfirmed() {
        val target = _pendingDeletion.value ?: return
        _pendingDeletion.value = null

        viewModelScope.launch {
            deleteRecording(DeleteRecordingUseCase.Params(target.id))
        }
    }

    private companion object {
        const val SUBSCRIPTION_TIMEOUT_MILLIS = 5_000L
    }
}
