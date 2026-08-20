package com.securevision.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securevision.core.common.extension.stateInWhileSubscribed
import com.securevision.core.common.result.getOrDefault
import com.securevision.core.domain.repository.SettingsRepository
import com.securevision.core.domain.usecase.data.ClearActivityDataUseCase
import com.securevision.core.domain.usecase.settings.ObserveSettingsUseCase
import com.securevision.core.model.AppSettings
import com.securevision.core.model.CameraResolution
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Reads and writes every user preference.
 *
 * Each control writes through a narrow setter and the screen re-renders from the
 * DataStore stream rather than from local state. That is what makes a setting
 * take effect immediately everywhere: the live camera reads the same stream per
 * frame, so a changed confidence threshold applies to the very next frame with no
 * restart and nothing to keep in sync.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    observeSettings: ObserveSettingsUseCase,
    private val repository: SettingsRepository,
    private val clearActivityData: ClearActivityDataUseCase,
) : ViewModel() {

    /** Current settings. */
    val uiState: StateFlow<SettingsUiState> = observeSettings(Unit)
        .map { result -> SettingsUiState.Ready(result.getOrDefault(AppSettings())) }
        .stateInWhileSubscribed(
            scope = viewModelScope,
            initialValue = SettingsUiState.Loading,
        )

    private val _clearOutcome = MutableStateFlow<ClearOutcome?>(null)

    /** Result of the most recent clear, shown once. */
    val clearOutcome: StateFlow<ClearOutcome?> = _clearOutcome.asStateFlow()

    /** @param enabled Post system notifications. */
    fun onPushNotificationsChange(enabled: Boolean) = write {
        repository.updatePushNotificationsEnabled(enabled)
    }

    /** @param enabled Play the alarm tone. */
    fun onAlertSoundChange(enabled: Boolean) = write {
        repository.updateAlertSoundEnabled(enabled)
    }

    /** @param enabled Vibrate on qualifying alerts. */
    fun onVibrationChange(enabled: Boolean) = write { repository.updateVibrationEnabled(enabled) }

    /** @param enabled Detect and recognise faces. */
    fun onFaceDetectionChange(enabled: Boolean) = write {
        repository.updateFaceDetectionEnabled(enabled)
    }

    /** @param enabled Detect weapons. */
    fun onWeaponDetectionChange(enabled: Boolean) = write {
        repository.updateWeaponDetectionEnabled(enabled)
    }

    /** @param enabled Alert on movement. */
    fun onMotionDetectionChange(enabled: Boolean) = write {
        repository.updateMotionDetectionEnabled(enabled)
    }

    /** @param enabled Infer soft attributes. */
    fun onAttributeAnalysisChange(enabled: Boolean) = write {
        repository.updateAttributeAnalysisEnabled(enabled)
    }

    /** @param threshold Minimum similarity for a match, in `0f..1f`. */
    fun onConfidenceThresholdChange(threshold: Float) = write {
        repository.updateConfidenceThreshold(threshold)
    }

    /** @param resolution Capture resolution for the live camera. */
    fun onCameraResolutionChange(resolution: CameraResolution) = write {
        repository.updateCameraResolution(resolution)
    }

    /** @param days How long alerts and events are kept. */
    fun onRetentionChange(days: Int) = write { repository.updateDataRetentionDays(days) }

    /** @param enabled Use the dark theme. */
    fun onDarkModeChange(enabled: Boolean) = write { repository.updateDarkMode(enabled) }

    /**
     * Deletes alerts, detection events and recordings.
     *
     * Enrolled people are deliberately untouched. Everything cleared here
     * regenerates from ordinary use; a face embedding does not — it is biometric
     * data held in one place with no backup, so removing it is a separate action
     * on the People screen where the confirmation can name what is lost.
     */
    fun onClearActivityData() {
        viewModelScope.launch {
            val result = clearActivityData(Unit)
            _clearOutcome.value = if (result.getOrDefault(false)) {
                ClearOutcome.CLEARED
            } else {
                ClearOutcome.FAILED
            }
        }
    }

    /** Clears the one-shot outcome once shown. */
    fun onClearOutcomeConsumed() {
        _clearOutcome.value = null
    }

    private fun write(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}

/** Outcome of a clear-data request. */
enum class ClearOutcome {

    /** Activity data was removed. */
    CLEARED,

    /** Something failed; nothing is claimed about what was removed. */
    FAILED,
}
