package com.securevision.feature.live

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.PersonAddAlt
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import com.securevision.core.domain.engine.EngineStatus
import com.securevision.core.domain.engine.EnrolmentCapture
import com.securevision.core.ui.component.SVEmptyState
import com.securevision.core.ui.theme.SecureVisionDimens
import com.securevision.core.ui.theme.SecureVisionTheme
import com.securevision.feature.live.camera.CameraPermissionGate
import com.securevision.feature.live.camera.CameraPreview
import com.securevision.feature.live.component.EnrolFaceDialog
import com.securevision.feature.live.component.LiveHud
import com.securevision.feature.live.component.MotionIndicator
import com.securevision.feature.live.component.SessionStatsBar
import com.securevision.feature.live.overlay.DetectionOverlay
import com.securevision.feature.live.overlay.OverlayTransform

/**
 * The live camera screen, rendered from state alone.
 *
 * @param uiState What to render.
 * @param enrolmentEvent Outcome of the most recent enrolment, shown once.
 * @param onFrame Called with each analysed frame.
 * @param onFlipCamera Switches lens.
 * @param onEnrol Enrols the face currently in frame.
 * @param onEnrolmentEventConsumed Clears the enrolment outcome once shown.
 * @param modifier Modifier applied to the screen.
 */
@Composable
fun LiveCameraScreen(
    uiState: LiveUiState,
    enrolmentEvent: EnrolmentEvent?,
    onFrame: (android.graphics.Bitmap, Boolean) -> Unit,
    onFlipCamera: () -> Unit,
    onEnrol: (name: String, age: Int) -> Unit,
    onEnrolmentEventConsumed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        LiveUiState.Loading -> Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }

        is LiveUiState.Error -> SVEmptyState(
            icon = Icons.Outlined.CloudOff,
            title = stringResource(R.string.live_recognition_offline_title),
            subtitle = uiState.message.orEmpty(),
            modifier = modifier.fillMaxSize(),
        )

        is LiveUiState.Ready -> CameraPermissionGate(modifier = modifier) {
            ReadyContent(
                state = uiState,
                enrolmentEvent = enrolmentEvent,
                onFrame = onFrame,
                onFlipCamera = onFlipCamera,
                onEnrol = onEnrol,
                onEnrolmentEventConsumed = onEnrolmentEventConsumed,
            )
        }
    }
}

@Composable
private fun ReadyContent(
    state: LiveUiState.Ready,
    enrolmentEvent: EnrolmentEvent?,
    onFrame: (android.graphics.Bitmap, Boolean) -> Unit,
    onFlipCamera: () -> Unit,
    onEnrol: (name: String, age: Int) -> Unit,
    onEnrolmentEventConsumed: () -> Unit,
) {
    var viewSize by remember { mutableStateOf(IntPair(0, 0)) }
    var showEnrolDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val enrolmentMessage = enrolmentEvent?.toMessage()

    LaunchedEffect(enrolmentEvent) {
        if (enrolmentMessage != null) {
            showEnrolDialog = false
            snackbarHostState.showSnackbar(enrolmentMessage)
            onEnrolmentEventConsumed()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { size -> viewSize = IntPair(size.width, size.height) },
    ) {
        CameraPreview(
            isFrontCamera = state.isFrontCamera,
            onFrame = { bitmap -> onFrame(bitmap, state.isFrontCamera) },
            modifier = Modifier.fillMaxSize(),
        )

        if (state.canProjectOverlay && viewSize.width > 0) {
            DetectionOverlay(
                detections = state.detections,
                weapons = state.weapons,
                transform = OverlayTransform(
                    analysisWidth = state.analysisWidth,
                    analysisHeight = state.analysisHeight,
                    viewWidth = viewSize.width.toFloat(),
                    viewHeight = viewSize.height.toFloat(),
                    isFrontCamera = state.isFrontCamera,
                ),
            )
        }

        Column(modifier = Modifier.align(Alignment.TopCenter)) {
            LiveHud(isFrontCamera = state.isFrontCamera, onFlipCamera = onFlipCamera)

            StatusBanner(state = state)
        }

        MotionIndicator(
            motion = state.motion,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(SecureVisionDimens.spacingMedium),
        )

        Column(
            modifier = Modifier.align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(SecureVisionDimens.spacingSmall),
        ) {
            ExtendedFloatingActionButton(
                onClick = { showEnrolDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(imageVector = Icons.Outlined.PersonAddAlt, contentDescription = null)
                Text(
                    text = stringResource(R.string.live_enrol_action),
                    modifier = Modifier.padding(start = SecureVisionDimens.spacingSmall),
                )
            }

            SessionStatsBar(stats = state.stats)
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    if (showEnrolDialog) {
        EnrolFaceDialog(
            isEnrolling = state.isEnrolling,
            onConfirm = onEnrol,
            onDismiss = { showEnrolDialog = false },
        )
    }
}

/**
 * Explains which detectors are not running, and why.
 *
 * Face and weapon are reported on their own lines rather than merged into one
 * "degraded" warning: they fail independently and the remedies differ — install a
 * face model, install a weapon model, or re-enrol profiles created with a
 * different model. A single line would leave the operator guessing which.
 */
@Composable
private fun StatusBanner(state: LiveUiState.Ready) {
    val messages = listOfNotNull(
        faceStatusMessage(state),
        weaponStatusMessage(state.weaponEngineStatus),
    )
    if (messages.isEmpty()) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SecureVisionTheme.colors.weaponContainer)
            .padding(SecureVisionDimens.spacingMediumSmall),
        horizontalArrangement = Arrangement.spacedBy(SecureVisionDimens.spacingSmall),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = Icons.Outlined.WarningAmber,
            contentDescription = null,
            modifier = Modifier.size(SecureVisionDimens.iconSmall),
            tint = SecureVisionTheme.colors.onWeaponContainer,
        )
        Column(verticalArrangement = Arrangement.spacedBy(SecureVisionDimens.spacingExtraSmall)) {
            messages.forEach { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = SecureVisionTheme.colors.onWeaponContainer,
                )
            }
        }
    }
}

@Composable
private fun faceStatusMessage(state: LiveUiState.Ready): String? =
    when (val status = state.faceEngineStatus) {
        is EngineStatus.RecognitionUnavailable -> when (status.reason) {
            EngineStatus.RecognitionUnavailable.Reason.MODEL_NOT_INSTALLED ->
                stringResource(R.string.live_recognition_missing_model)
            EngineStatus.RecognitionUnavailable.Reason.MODEL_LOAD_FAILED ->
                stringResource(R.string.live_recognition_load_failed)
            EngineStatus.RecognitionUnavailable.Reason.EMBEDDING_DIMENSION_MISMATCH ->
                stringResource(R.string.live_recognition_dimension_mismatch)
        }

        is EngineStatus.Ready -> if (state.enrolledCount == 0) {
            stringResource(R.string.live_no_profiles)
        } else {
            null
        }

        EngineStatus.Initialising -> null
    }

@Composable
private fun weaponStatusMessage(status: EngineStatus): String? = when (status) {
    is EngineStatus.RecognitionUnavailable -> when (status.reason) {
        EngineStatus.RecognitionUnavailable.Reason.MODEL_NOT_INSTALLED ->
            stringResource(R.string.live_weapon_missing_model)
        // A weapon model that fails to load and one whose shapes are wrong are
        // the same problem to the operator: the file is not usable, replace it.
        EngineStatus.RecognitionUnavailable.Reason.MODEL_LOAD_FAILED,
        EngineStatus.RecognitionUnavailable.Reason.EMBEDDING_DIMENSION_MISMATCH,
        -> stringResource(R.string.live_weapon_load_failed)
    }

    is EngineStatus.Ready, EngineStatus.Initialising -> null
}

@Composable
private fun EnrolmentEvent.toMessage(): String = when (this) {
    is EnrolmentEvent.Enrolled -> stringResource(R.string.live_enrol_success, name)
    is EnrolmentEvent.Failed -> when (reason) {
        EnrolmentCapture.Failure.Reason.NO_FACE_DETECTED ->
            stringResource(R.string.live_enrol_error_no_face)
        EnrolmentCapture.Failure.Reason.MULTIPLE_FACES ->
            stringResource(R.string.live_enrol_error_multiple_faces)
        EnrolmentCapture.Failure.Reason.POOR_QUALITY ->
            stringResource(R.string.live_enrol_error_poor_quality)
        EnrolmentCapture.Failure.Reason.LANDMARKS_UNAVAILABLE ->
            stringResource(R.string.live_enrol_error_landmarks)
        EnrolmentCapture.Failure.Reason.MODEL_UNAVAILABLE ->
            stringResource(R.string.live_enrol_error_model)
        null -> stringResource(R.string.live_enrol_error_generic)
    }
}

/** Avoids boxing a pair of ints on every layout pass. */
private data class IntPair(val width: Int, val height: Int)
