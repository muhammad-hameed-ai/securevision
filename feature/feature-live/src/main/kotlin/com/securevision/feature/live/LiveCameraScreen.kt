package com.securevision.feature.live

import android.content.Context
import androidx.camera.core.Camera
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
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.securevision.core.common.Constants
import com.securevision.core.domain.engine.EngineStatus
import com.securevision.core.model.Recording
import com.securevision.core.ui.component.SVEmptyState
import com.securevision.core.ui.theme.SecureVisionDimens
import com.securevision.core.ui.theme.SecureVisionTheme
import com.securevision.feature.live.camera.BindOutcome
import com.securevision.feature.live.camera.CameraPermissionGate
import com.securevision.feature.live.camera.CameraPreview
import com.securevision.feature.live.camera.NotificationPermissionRequest
import com.securevision.feature.live.camera.VideoRecorder
import com.securevision.feature.live.component.LiveHud
import com.securevision.feature.live.component.RecordButton
import com.securevision.feature.live.component.SessionStatsBar
import com.securevision.feature.live.component.SilenceAlarmButton
import com.securevision.feature.live.overlay.DetectionOverlay
import com.securevision.feature.live.overlay.OverlayTransform
import java.io.File
import kotlinx.coroutines.delay

/**
 * The live camera screen, rendered from state alone.
 *
 * @param uiState What to render.
 * @param onFrame Called with each analysed frame.
 * @param onFlipCamera Switches lens.
 * @param onSilenceAlarm Stops a sounding critical alarm.
 * @param onRecordingStateChange Reports the recorder's state upward.
 * @param onRecordingFinished Hands a completed clip to be saved.
 * @param onCameraBound Reports that a lens change has finished binding.
 * @param modifier Modifier applied to the screen.
 */
@Composable
fun LiveCameraScreen(
    uiState: LiveUiState,
    onFrame: (android.graphics.Bitmap, Boolean) -> Unit,
    onFlipCamera: () -> Unit,
    onSilenceAlarm: () -> Unit,
    onRecordingStateChange: (Boolean, Long) -> Unit,
    onRecordingFinished: (Recording) -> Unit,
    onCameraBound: (String) -> Unit,
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
            // Asked inside the camera gate, so the two system dialogs appear one
            // after the other rather than stacked on top of each other.
            NotificationPermissionRequest()

            ReadyContent(
                state = uiState,
                onFrame = onFrame,
                onFlipCamera = onFlipCamera,
                onSilenceAlarm = onSilenceAlarm,
                onRecordingStateChange = onRecordingStateChange,
                onRecordingFinished = onRecordingFinished,
                onCameraBound = onCameraBound,
            )
        }
    }
}

@Composable
private fun ReadyContent(
    state: LiveUiState.Ready,
    onFrame: (android.graphics.Bitmap, Boolean) -> Unit,
    onFlipCamera: () -> Unit,
    onSilenceAlarm: () -> Unit,
    onRecordingStateChange: (Boolean, Long) -> Unit,
    onRecordingFinished: (Recording) -> Unit,
    onCameraBound: (String) -> Unit,
) {
    val context = LocalContext.current
    var viewSize by remember { mutableStateOf(IntPair(0, 0)) }
    val snackbarHostState = remember { SnackbarHostState() }

    val recorder = remember(context) { VideoRecorder(context) }
    var bindOutcome by remember { mutableStateOf(BindOutcome.PREVIEW_ONLY) }

    // Held here rather than in the ViewModel: the torch is a property of the
    // bound camera, resets when the screen goes away, and is deliberately not
    // persisted — a light that switches itself back on later would be a surprise.
    var camera by remember { mutableStateOf<Camera?>(null) }
    var hasTorch by remember { mutableStateOf(false) }
    var isTorchOn by remember { mutableStateOf(false) }

    // Ticks the on-screen timer. Driven here rather than from the recorder so the
    // clock is a function of state and stops the moment recording does.
    LaunchedEffect(state.isRecording) {
        while (state.isRecording) {
            onRecordingStateChange(true, recorder.elapsedMillis())
            delay(TIMER_TICK_MILLIS)
        }
    }

    DisposableEffect(recorder) {
        // A recording left open when the screen goes away would keep writing to a
        // file nothing is tracking.
        onDispose { if (recorder.isRecording) recorder.stop() }
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
            recorder = recorder,
            onBindResult = { outcome ->
                bindOutcome = outcome
                // Ends the switch on the bind, not on a frame. A camera that has
                // bound but not yet produced an image is still a completed
                // switch, and tying the spinner to analysis is what let a stalled
                // pass keep the control busy.
                onCameraBound(outcome.name)
            },
            onCameraReady = { bound, torchAvailable ->
                camera = bound
                hasTorch = torchAvailable
                // A lens change drops the torch, so the icon must not keep
                // claiming it is lit.
                isTorchOn = false
            },
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
            // The motion meter lives inside the HUD row rather than floating
            // against the same edge as the flip control, which is what made the
            // flip button hard to hit.
            LiveHud(
                isFrontCamera = state.isFrontCamera,
                motion = state.motion,
                isSwitchingCamera = state.isSwitchingCamera,
                hasTorch = hasTorch,
                isTorchOn = isTorchOn,
                onToggleTorch = {
                    val next = !isTorchOn
                    camera?.cameraControl?.enableTorch(next)
                    isTorchOn = next
                },
                onFlipCamera = onFlipCamera,
            )

            StatusBanner(state = state)
        }

        Column(
            modifier = Modifier.align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(SecureVisionDimens.spacingSmall),
        ) {
            SilenceAlarmButton(
                isSounding = state.isAlarmSounding,
                onSilence = onSilenceAlarm,
            )

            RecordButton(
                isRecording = state.isRecording,
                elapsedMillis = state.recordingElapsedMillis,
                enabled = bindOutcome == BindOutcome.FULL ||
                    bindOutcome == BindOutcome.VIDEO_WITHOUT_ANALYSIS,
                onToggle = {
                    if (recorder.isRecording) {
                        recorder.stop()
                    } else {
                        startRecording(
                            recorder = recorder,
                            context = context,
                            onStarted = { onRecordingStateChange(true, 0L) },
                            onFinished = { recording ->
                                onRecordingStateChange(false, 0L)
                                recording?.let(onRecordingFinished)
                            },
                        )
                    }
                },
            )

            SessionStatsBar(stats = state.stats)
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

/**
 * Allocates a destination and starts capture.
 *
 * The file is created through the app's own private-storage path rather than
 * anywhere shared: a clip may contain footage of people's faces, and anything in
 * external storage is readable by other apps and indexed by the gallery.
 */
private fun startRecording(
    recorder: VideoRecorder,
    context: Context,
    onStarted: () -> Unit,
    onFinished: (Recording?) -> Unit,
) {
    val directory = File(context.filesDir, Constants.Storage.RECORDING_DIRECTORY).apply {
        if (!exists()) mkdirs()
    }

    val destination = File(
        directory,
        "recording_${System.currentTimeMillis()}.${Constants.Storage.VIDEO_EXTENSION}",
    )

    onStarted()
    recorder.start(destination, onFinished)
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
        // Stated plainly rather than hidden: the alerts are still being recorded,
        // and a user who refused the permission should know the phone staying
        // quiet is their setting rather than the app failing.
        stringResource(R.string.live_notifications_blocked)
            .takeIf { state.notificationsBlocked },
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

/** Avoids boxing a pair of ints on every layout pass. */
private data class IntPair(val width: Int, val height: Int)

/** Timer refresh. Fast enough to look live, slow enough not to recompose wastefully. */
private const val TIMER_TICK_MILLIS = 500L
