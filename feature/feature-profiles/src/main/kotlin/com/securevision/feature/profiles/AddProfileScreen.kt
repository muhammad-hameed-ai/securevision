package com.securevision.feature.profiles

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import com.securevision.core.domain.engine.EnrolmentCapture
import com.securevision.core.model.AccessLevel
import com.securevision.core.ui.component.SVChip
import com.securevision.core.ui.component.SVPrimaryButton
import com.securevision.core.ui.component.SVTextField
import com.securevision.core.ui.component.SVTopBar
import com.securevision.core.ui.theme.SecureVisionDimens
import com.securevision.core.ui.theme.SecureVisionTheme
import com.securevision.feature.profiles.camera.EnrolmentCamera
import com.securevision.feature.profiles.camera.EnrolmentCameraController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Enrol someone new, or re-enrol an existing person's face.
 *
 * The aligned crop is shown before saving. That is the point of the screen: a
 * badly framed enrolment is visible here in a second, whereas its only other
 * symptom is match scores that quietly never quite reach the threshold.
 *
 * @param uiState Current form state.
 * @param onNameChange Name edited.
 * @param onAgeChange Age edited.
 * @param onAccessLevelChange Classification selected.
 * @param onWatchlistToggle Watchlist flag toggled.
 * @param onPhotoCaptured A photo was taken.
 * @param onRetakePhoto Discards the capture.
 * @param onSave Stores the profile.
 * @param onBack Leaves without saving.
 * @param modifier Modifier applied to the screen.
 */
@Composable
fun AddProfileScreen(
    uiState: AddProfileUiState,
    onNameChange: (String) -> Unit,
    onAgeChange: (String) -> Unit,
    onAccessLevelChange: (AccessLevel) -> Unit,
    onWatchlistToggle: () -> Unit,
    onPhotoCaptured: (android.graphics.Bitmap, Boolean) -> Unit,
    onRetakePhoto: () -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val controller = remember { EnrolmentCameraController() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var isFrontCamera by remember { mutableStateOf(true) }

    // PickVisualMedia, not OpenDocument: the system photo picker needs no storage
    // permission at all, which keeps the app's three-permission list intact.
    val galleryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult

        scope.launch {
            context.decodeForEnrolment(uri)?.let { bitmap ->
                // A gallery photo has no lens; false means "do not mirror", which
                // is correct for an image that was already saved upright.
                onPhotoCaptured(bitmap, false)
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            SVTopBar(
                title = stringResource(
                    if (uiState.isEditing) R.string.profiles_edit_title else R.string.profiles_add,
                ),
                onBack = onBack,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(SecureVisionDimens.spacingMedium),
            verticalArrangement = Arrangement.spacedBy(SecureVisionDimens.spacingMedium),
        ) {
            CaptureArea(
                uiState = uiState,
                controller = controller,
                isFrontCamera = isFrontCamera,
                onCapture = {
                    scope.launch {
                        controller.capture()?.let { bitmap ->
                            onPhotoCaptured(bitmap, isFrontCamera)
                        }
                    }
                },
                onRetake = onRetakePhoto,
                onFlip = { isFrontCamera = !isFrontCamera },
                onPickFromGallery = {
                    galleryPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
            )

            SVTextField(
                value = uiState.name,
                onValueChange = onNameChange,
                label = stringResource(R.string.profiles_field_name),
                error = uiState.nameError?.message(),
                modifier = Modifier.fillMaxWidth(),
            )

            SVTextField(
                value = uiState.age,
                onValueChange = onAgeChange,
                label = stringResource(R.string.profiles_field_age),
                error = uiState.ageError?.message(),
                modifier = Modifier.fillMaxWidth(),
            )

            Text(
                text = stringResource(R.string.profiles_field_access),
                style = MaterialTheme.typography.labelLarge,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(SecureVisionDimens.spacingSmall)) {
                AccessLevel.entries.forEach { level ->
                    SVChip(
                        label = stringResource(level.labelRes()),
                        selected = uiState.accessLevel == level,
                        onClick = { onAccessLevelChange(level) },
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.profiles_field_watchlist),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = stringResource(R.string.profiles_field_watchlist_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Switch(checked = uiState.isWatchlisted, onCheckedChange = { onWatchlistToggle() })
            }

            SVPrimaryButton(
                text = stringResource(R.string.profiles_save),
                onClick = onSave,
                enabled = uiState.canSave,
                loading = uiState.isSaving,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun CaptureArea(
    uiState: AddProfileUiState,
    controller: EnrolmentCameraController,
    isFrontCamera: Boolean,
    onCapture: () -> Unit,
    onRetake: () -> Unit,
    onFlip: () -> Unit,
    onPickFromGallery: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(CAPTURE_ASPECT)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        val crop = uiState.alignedCrop

        if (crop != null) {
            // The aligned crop, at the size the model saw it. Shown large so a
            // rotated or half-cropped face is obvious.
            Image(
                bitmap = crop.asImageBitmap(),
                contentDescription = stringResource(R.string.profiles_aligned_preview),
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(SecureVisionDimens.spacingMedium),
            )
        } else {
            EnrolmentCamera(
                controller = controller,
                isFrontCamera = isFrontCamera,
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (uiState.isCapturing || uiState.isPreparing) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    }

    if (uiState.isPreparing) {
        StatusLine(
            icon = Icons.Outlined.Refresh,
            text = stringResource(R.string.profiles_preparing_model),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    // Only said once the model has actually been asked for and refused. Saying it
    // before that — as this screen did when nothing ever called prepare — blamed a
    // missing file for a request that was never made.
    if (uiState.modelUnavailable) {
        StatusLine(
            icon = Icons.Outlined.ErrorOutline,
            text = stringResource(R.string.profiles_error_model),
            tint = MaterialTheme.colorScheme.error,
        )
    }

    uiState.captureFailure?.let { reason ->
        StatusLine(
            icon = Icons.Outlined.ErrorOutline,
            text = reason.message(),
            tint = MaterialTheme.colorScheme.error,
        )
    }

    if (uiState.hasFace) {
        StatusLine(
            icon = Icons.Outlined.CheckCircle,
            text = stringResource(R.string.profiles_face_enrolled_confirm),
            tint = SecureVisionTheme.colors.known,
        )
    }

    Row(horizontalArrangement = Arrangement.spacedBy(SecureVisionDimens.spacingSmall)) {
        if (uiState.hasFace) {
            FilledTonalButton(onClick = onRetake) {
                Icon(imageVector = Icons.Outlined.Refresh, contentDescription = null)
                Text(
                    text = stringResource(R.string.profiles_retake),
                    modifier = Modifier.padding(start = SecureVisionDimens.spacingSmall),
                )
            }
        } else {
            FilledTonalButton(onClick = onCapture, enabled = uiState.canCapture) {
                Icon(imageVector = Icons.Outlined.CameraAlt, contentDescription = null)
                Text(
                    text = stringResource(R.string.profiles_capture),
                    modifier = Modifier.padding(start = SecureVisionDimens.spacingSmall),
                )
            }

            FilledTonalButton(onClick = onFlip) {
                Text(stringResource(R.string.profiles_flip_camera))
            }

            // A posed, well-lit gallery photo usually aligns better than a live
            // frame, so this tends to produce the stronger embedding of the two.
            FilledTonalButton(onClick = onPickFromGallery) {
                Icon(imageVector = Icons.Outlined.PhotoLibrary, contentDescription = null)
                Text(
                    text = stringResource(R.string.profiles_choose_gallery),
                    modifier = Modifier.padding(start = SecureVisionDimens.spacingSmall),
                )
            }
        }
    }
}

/**
 * Decodes a picked image at a size the face pipeline can use.
 *
 * Downsampled and decoded off the main thread: a 12-megapixel gallery photo
 * decoded full-size on the UI thread is an out-of-memory crash on a mid-range
 * device, and the detector gains nothing from pixels beyond this bound.
 *
 * @param uri The picked image.
 * @return An upright bitmap, or `null` if it could not be read.
 */
private suspend fun Context.decodeForEnrolment(uri: Uri): Bitmap? =
    withContext(Dispatchers.IO) {
        runCatching {
            // BitmapFactory, not ImageDecoder: the latter is API 28 and this app
            // supports 26. It would have crashed on Android 8 rather than
            // degrading.
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
            }

            val longestEdge = maxOf(bounds.outWidth, bounds.outHeight)
            if (longestEdge <= 0) return@runCatching null

            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSizeFor(longestEdge)
                inMutable = true
            }

            val decoded = contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            } ?: return@runCatching null

            // BitmapFactory ignores EXIF orientation, unlike ImageDecoder. A
            // portrait photo would arrive on its side, and a sideways face gives
            // the aligner landmarks it cannot square up — the enrolment would
            // fail for a photo that looks perfectly good in the gallery.
            decoded.rotated(exifRotationOf(uri))
        }.onFailure { throwable ->
            Log.w(TAG, "could not decode the picked image", throwable)
        }.getOrNull()
    }

/** Largest power of two that keeps the longest edge within [MAX_DECODE_EDGE]. */
private fun sampleSizeFor(longestEdge: Int): Int {
    var sample = 1
    while (longestEdge / (sample * 2) >= MAX_DECODE_EDGE) sample *= 2

    return sample
}

/**
 * Reads the EXIF orientation a camera app recorded when the photo was taken.
 *
 * @return Degrees to rotate clockwise, or zero when there is no usable tag.
 */
private fun Context.exifRotationOf(uri: Uri): Int = runCatching {
    contentResolver.openInputStream(uri)?.use { stream ->
        when (
            ExifInterface(stream).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
        ) {
            ExifInterface.ORIENTATION_ROTATE_90 -> QUARTER_TURN
            ExifInterface.ORIENTATION_ROTATE_180 -> HALF_TURN
            ExifInterface.ORIENTATION_ROTATE_270 -> THREE_QUARTER_TURN
            else -> 0
        }
    } ?: 0
}.getOrDefault(0)

/** Rotates a bitmap clockwise, returning the original when there is nothing to do. */
private fun Bitmap.rotated(degrees: Int): Bitmap {
    if (degrees == 0) return this

    val matrix = Matrix().apply { postRotate(degrees.toFloat()) }

    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

private const val QUARTER_TURN = 90
private const val HALF_TURN = 180
private const val THREE_QUARTER_TURN = 270
private const val MAX_DECODE_EDGE = 1_600
private const val TAG = "AddProfileScreen"

@Composable
private fun StatusLine(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    tint: androidx.compose.ui.graphics.Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SecureVisionDimens.spacingSmall),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(SecureVisionDimens.iconSmall),
        )
        Text(text = text, style = MaterialTheme.typography.bodySmall, color = tint)
    }
}

/** Maps a validation failure onto localised copy. */
@Composable
private fun FieldError.message(): String = stringResource(
    when (this) {
        FieldError.REQUIRED -> R.string.profiles_error_required
        FieldError.AGE_OUT_OF_RANGE -> R.string.profiles_error_age_range
    },
)

/**
 * Maps a capture failure onto copy that says what to change.
 *
 * Each reason gets its own sentence: "no face" and "too far away" need different
 * corrections, and a single generic message would leave the operator retaking the
 * same bad photo.
 */
@Composable
private fun EnrolmentCapture.Failure.Reason.message(): String = stringResource(
    when (this) {
        EnrolmentCapture.Failure.Reason.NO_FACE_DETECTED -> R.string.profiles_error_no_face
        EnrolmentCapture.Failure.Reason.MULTIPLE_FACES -> R.string.profiles_error_multiple_faces
        EnrolmentCapture.Failure.Reason.POOR_QUALITY -> R.string.profiles_error_poor_quality
        EnrolmentCapture.Failure.Reason.LANDMARKS_UNAVAILABLE -> R.string.profiles_error_landmarks
        EnrolmentCapture.Failure.Reason.MODEL_UNAVAILABLE -> R.string.profiles_error_model
    },
)

/** The display name for a classification. */
internal fun AccessLevel.labelRes(): Int = when (this) {
    AccessLevel.STANDARD -> R.string.profiles_access_standard
    AccessLevel.RESTRICTED -> R.string.profiles_access_restricted
    AccessLevel.VIP -> R.string.profiles_access_vip
}

private const val CAPTURE_ASPECT = 3f / 4f
