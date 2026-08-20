package com.securevision.feature.settings

import androidx.compose.runtime.Immutable
import com.securevision.core.model.AppSettings

/** What the settings screen renders. */
@Immutable
sealed interface SettingsUiState {

    /** The stored preferences have not been read yet. */
    data object Loading : SettingsUiState

    /**
     * Preferences loaded.
     *
     * Carries the whole [AppSettings] rather than a field per control: the screen
     * is a direct view of the stored object, and mirroring each value into its own
     * property would create a second place for them to disagree.
     *
     * @property settings Current preferences.
     */
    @Immutable
    data class Ready(val settings: AppSettings) : SettingsUiState
}
