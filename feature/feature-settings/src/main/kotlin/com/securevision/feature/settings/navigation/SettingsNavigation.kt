package com.securevision.feature.settings.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.securevision.feature.settings.SettingsCallbacks
import com.securevision.feature.settings.SettingsRoutes
import com.securevision.feature.settings.SettingsScreen
import com.securevision.feature.settings.SettingsViewModel

/**
 * Registers the settings destination.
 *
 * @param appVersion Version name from the application module, which is the only
 *   place `BuildConfig` for the app itself exists.
 */
fun NavGraphBuilder.settingsScreen(appVersion: String) {
    composable(route = SettingsRoutes.SETTINGS) {
        SettingsRoute(appVersion = appVersion)
    }
}

@Composable
private fun SettingsRoute(appVersion: String) {
    val viewModel: SettingsViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val clearOutcome by viewModel.clearOutcome.collectAsStateWithLifecycle()

    SettingsScreen(
        uiState = uiState,
        clearOutcome = clearOutcome,
        appVersion = appVersion,
        callbacks = SettingsCallbacks(
            onPushNotificationsChange = viewModel::onPushNotificationsChange,
            onAlertSoundChange = viewModel::onAlertSoundChange,
            onVibrationChange = viewModel::onVibrationChange,
            onFaceDetectionChange = viewModel::onFaceDetectionChange,
            onWeaponDetectionChange = viewModel::onWeaponDetectionChange,
            onMotionDetectionChange = viewModel::onMotionDetectionChange,
            onAttributeAnalysisChange = viewModel::onAttributeAnalysisChange,
            onConfidenceThresholdChange = viewModel::onConfidenceThresholdChange,
            onCameraResolutionChange = viewModel::onCameraResolutionChange,
            onRetentionChange = viewModel::onRetentionChange,
            onDarkModeChange = viewModel::onDarkModeChange,
            onClearActivityData = viewModel::onClearActivityData,
            onClearOutcomeConsumed = viewModel::onClearOutcomeConsumed,
        ),
    )
}
