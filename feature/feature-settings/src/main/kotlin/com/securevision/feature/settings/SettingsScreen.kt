package com.securevision.feature.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.securevision.core.model.AppSettings
import com.securevision.core.model.CameraResolution
import com.securevision.core.ui.component.SVTopBar
import com.securevision.core.ui.theme.SecureVisionDimens
import com.securevision.feature.settings.component.SettingsInfoRow
import com.securevision.feature.settings.component.SettingsRadioOption
import com.securevision.feature.settings.component.SettingsSection
import com.securevision.feature.settings.component.SettingsSlider
import com.securevision.feature.settings.component.SettingsToggle
import kotlin.math.roundToInt

/**
 * Every user preference, in one screen.
 *
 * @param uiState Current preferences.
 * @param clearOutcome Result of the most recent clear, shown once.
 * @param callbacks Handlers for each control.
 * @param appVersion Version name, read from the build rather than typed in.
 * @param modifier Modifier applied to the screen.
 */
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    clearOutcome: ClearOutcome?,
    callbacks: SettingsCallbacks,
    appVersion: String,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val clearedMessage = stringResource(R.string.settings_data_cleared)
    val failedMessage = stringResource(R.string.settings_data_clear_failed)

    LaunchedEffect(clearOutcome) {
        when (clearOutcome) {
            ClearOutcome.CLEARED -> snackbarHostState.showSnackbar(clearedMessage)
            ClearOutcome.FAILED -> snackbarHostState.showSnackbar(failedMessage)
            null -> return@LaunchedEffect
        }
        callbacks.onClearOutcomeConsumed()
    }

    Scaffold(
        modifier = modifier,
        topBar = { SVTopBar(title = stringResource(R.string.settings_title)) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (uiState) {
                SettingsUiState.Loading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary,
                )

                is SettingsUiState.Ready -> SettingsContent(
                    settings = uiState.settings,
                    callbacks = callbacks,
                    appVersion = appVersion,
                )
            }
        }
    }
}

@Composable
private fun SettingsContent(
    settings: AppSettings,
    callbacks: SettingsCallbacks,
    appVersion: String,
) {
    var showClearDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = SecureVisionDimens.spacingMedium),
    ) {
        SettingsSection(stringResource(R.string.settings_section_notifications))

        SettingsToggle(
            title = stringResource(R.string.settings_push),
            subtitle = stringResource(R.string.settings_push_hint),
            checked = settings.pushNotificationsEnabled,
            onCheckedChange = callbacks.onPushNotificationsChange,
        )
        SettingsToggle(
            title = stringResource(R.string.settings_sound),
            subtitle = stringResource(R.string.settings_sound_hint),
            checked = settings.alertSoundEnabled,
            onCheckedChange = callbacks.onAlertSoundChange,
        )
        SettingsToggle(
            title = stringResource(R.string.settings_vibration),
            subtitle = stringResource(R.string.settings_vibration_hint),
            checked = settings.vibrationEnabled,
            onCheckedChange = callbacks.onVibrationChange,
        )

        HorizontalDivider()
        SettingsSection(stringResource(R.string.settings_section_detection))

        SettingsToggle(
            title = stringResource(R.string.settings_face),
            subtitle = stringResource(R.string.settings_face_hint),
            checked = settings.faceDetectionEnabled,
            onCheckedChange = callbacks.onFaceDetectionChange,
        )
        SettingsToggle(
            title = stringResource(R.string.settings_weapon),
            subtitle = stringResource(R.string.settings_weapon_hint),
            checked = settings.weaponDetectionEnabled,
            onCheckedChange = callbacks.onWeaponDetectionChange,
        )
        SettingsToggle(
            title = stringResource(R.string.settings_motion),
            subtitle = stringResource(R.string.settings_motion_hint),
            checked = settings.motionDetectionEnabled,
            onCheckedChange = callbacks.onMotionDetectionChange,
        )
        SettingsToggle(
            title = stringResource(R.string.settings_attributes),
            subtitle = stringResource(R.string.settings_attributes_hint),
            checked = settings.attributeAnalysisEnabled,
            onCheckedChange = callbacks.onAttributeAnalysisChange,
        )

        SettingsSlider(
            title = stringResource(R.string.settings_confidence),
            valueLabel = "${(settings.confidenceThreshold * PERCENT).roundToInt()}%",
            value = settings.confidenceThreshold,
            onValueChange = callbacks.onConfidenceThresholdChange,
            valueRange = CONFIDENCE_MIN..CONFIDENCE_MAX,
        )
        Text(
            text = stringResource(R.string.settings_confidence_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        HorizontalDivider(modifier = Modifier.padding(top = SecureVisionDimens.spacingMedium))
        SettingsSection(stringResource(R.string.settings_section_camera))

        CameraResolution.entries.forEach { resolution ->
            SettingsRadioOption(
                label = stringResource(resolution.labelRes()),
                selected = settings.cameraResolution == resolution,
                onSelect = { callbacks.onCameraResolutionChange(resolution) },
            )
        }

        HorizontalDivider()
        SettingsSection(stringResource(R.string.settings_section_data))

        SettingsSlider(
            title = stringResource(R.string.settings_retention),
            valueLabel = stringResource(R.string.settings_retention_days, settings.dataRetentionDays),
            value = RETENTION_CHOICES.indexOf(settings.dataRetentionDays)
                .coerceAtLeast(0)
                .toFloat(),
            onValueChange = { position ->
                callbacks.onRetentionChange(RETENTION_CHOICES[position.roundToInt()])
            },
            valueRange = 0f..(RETENTION_CHOICES.size - 1).toFloat(),
            // Discrete stops: 7, 30 and 90 days are the only meaningful choices,
            // and a continuous slider would invite "43 days" as if it mattered.
            steps = RETENTION_CHOICES.size - 2,
        )

        OutlinedButton(
            onClick = { showClearDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = SecureVisionDimens.spacingSmall),
        ) {
            Text(
                text = stringResource(R.string.settings_clear_data),
                color = MaterialTheme.colorScheme.error,
            )
        }

        HorizontalDivider()
        SettingsSection(stringResource(R.string.settings_section_appearance))

        SettingsToggle(
            title = stringResource(R.string.settings_dark_mode),
            subtitle = stringResource(R.string.settings_dark_mode_hint),
            checked = settings.darkMode,
            onCheckedChange = callbacks.onDarkModeChange,
        )

        HorizontalDivider()
        SettingsSection(stringResource(R.string.settings_section_about))

        SettingsInfoRow(
            label = stringResource(R.string.settings_version),
            value = appVersion,
        )
        SettingsInfoRow(
            label = stringResource(R.string.settings_permissions),
            value = stringResource(R.string.settings_permissions_value),
        )
        Text(
            text = stringResource(R.string.settings_on_device),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = SecureVisionDimens.spacingExtraLarge),
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(stringResource(R.string.settings_clear_dialog_title)) },
            // Names exactly what goes and what stays. "Clear all data" next to a
            // list of enrolled faces would otherwise read as though it wipes them.
            text = { Text(stringResource(R.string.settings_clear_dialog_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        callbacks.onClearActivityData()
                    },
                ) {
                    Text(
                        text = stringResource(R.string.settings_clear_confirm),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(stringResource(R.string.settings_cancel))
                }
            },
        )
    }
}

/** Display name for a capture resolution. */
private fun CameraResolution.labelRes(): Int = when (this) {
    CameraResolution.SD_480 -> R.string.settings_resolution_standard
    CameraResolution.HD_720 -> R.string.settings_resolution_hd
    CameraResolution.FHD_1080 -> R.string.settings_resolution_fhd
}

/**
 * Handlers for the settings controls.
 *
 * Grouped into one object rather than seventeen parameters — a positional
 * argument list that long is a place for two lambdas to be silently swapped.
 */
data class SettingsCallbacks(
    val onPushNotificationsChange: (Boolean) -> Unit,
    val onAlertSoundChange: (Boolean) -> Unit,
    val onVibrationChange: (Boolean) -> Unit,
    val onFaceDetectionChange: (Boolean) -> Unit,
    val onWeaponDetectionChange: (Boolean) -> Unit,
    val onMotionDetectionChange: (Boolean) -> Unit,
    val onAttributeAnalysisChange: (Boolean) -> Unit,
    val onConfidenceThresholdChange: (Float) -> Unit,
    val onCameraResolutionChange: (CameraResolution) -> Unit,
    val onRetentionChange: (Int) -> Unit,
    val onDarkModeChange: (Boolean) -> Unit,
    val onClearActivityData: () -> Unit,
    val onClearOutcomeConsumed: () -> Unit,
)

/** Retention options in days. */
private val RETENTION_CHOICES = listOf(7, 30, 90)

private const val PERCENT = 100f

/**
 * Bounds for the match threshold.
 *
 * Not 0..1: below roughly half, cosine similarity stops discriminating between
 * people at all, and a slider that offers "match anyone" is offering to break
 * recognition.
 */
private const val CONFIDENCE_MIN = 0.5f
private const val CONFIDENCE_MAX = 0.95f
