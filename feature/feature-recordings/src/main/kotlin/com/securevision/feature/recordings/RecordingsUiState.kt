package com.securevision.feature.recordings

import androidx.compose.runtime.Immutable
import com.securevision.core.model.Recording

/** What the recordings gallery renders. */
@Immutable
sealed interface RecordingsUiState {

    /** The first load has not produced a list yet. */
    data object Loading : RecordingsUiState

    /**
     * Clips are being shown.
     *
     * @property recordings Newest first.
     */
    @Immutable
    data class Content(val recordings: List<Recording>) : RecordingsUiState

    /** Nothing has been recorded. */
    data object Empty : RecordingsUiState
}
