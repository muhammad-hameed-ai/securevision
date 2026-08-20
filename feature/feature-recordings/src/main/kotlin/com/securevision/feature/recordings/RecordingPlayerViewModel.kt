package com.securevision.feature.recordings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securevision.core.common.result.getOrDefault
import com.securevision.core.domain.usecase.invoke
import com.securevision.core.domain.usecase.recording.GetRecordingsUseCase
import com.securevision.core.model.Recording
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Resolves one clip for the player.
 *
 * Looks the recording up itself rather than filtering the gallery's list. The
 * gallery's state starts as `Loading`, so a player that read from it showed
 * "recording unavailable" for a perfectly good file whenever it was opened
 * before that first emission — including on process death, when the player is
 * restored without the gallery ever having been on screen.
 */
@HiltViewModel
class RecordingPlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getRecordings: GetRecordingsUseCase,
) : ViewModel() {

    private val _recording = MutableStateFlow<Recording?>(null)

    /** The clip to play, or `null` once loading has finished without finding it. */
    val recording: StateFlow<Recording?> = _recording.asStateFlow()

    private val _isLoading = MutableStateFlow(true)

    /** Whether the lookup is still running, so "unavailable" is not shown early. */
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        val id: String? = savedStateHandle[RecordingsRoutes.ARG_RECORDING_ID]

        viewModelScope.launch {
            // `first()` rather than a subscription: the file path of an existing
            // clip does not change, and the player has no reason to react to the
            // rest of the gallery.
            _recording.value = getRecordings()
                .first()
                .getOrDefault(emptyList())
                .firstOrNull { candidate -> candidate.id == id }

            _isLoading.value = false
        }
    }
}
