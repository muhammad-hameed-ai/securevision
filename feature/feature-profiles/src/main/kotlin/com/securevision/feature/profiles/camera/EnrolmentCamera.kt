package com.securevision.feature.profiles.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * A still-capture camera for enrolment.
 *
 * Uses `ImageCapture`, not the continuous `ImageAnalysis` stream the live screen
 * runs. Enrolment wants one deliberate, well-exposed photo of a person who is
 * standing still and cooperating; monitoring wants a cheap frame every few
 * hundred milliseconds. Sharing one component would force both to compromise.
 *
 * @param controller Handle the caller uses to trigger a capture.
 * @param isFrontCamera Which lens to bind.
 * @param modifier Modifier applied to the preview surface.
 */
@Composable
fun EnrolmentCamera(
    controller: EnrolmentCameraController,
    isFrontCamera: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    LaunchedEffect(isFrontCamera) {
        runCatching {
            val provider = context.awaitCameraProvider()

            val preview = Preview.Builder().build().apply {
                setSurfaceProvider(previewView.surfaceProvider)
            }

            val capture = ImageCapture.Builder()
                // Quality over latency: this photo becomes a face embedding that
                // every future match is compared against, so a slightly slower
                // shutter is a good trade for a sharper crop.
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build()

            val selector = if (isFrontCamera) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }

            provider.unbindAll()
            provider.bindToLifecycle(lifecycleOwner, selector, preview, capture)

            controller.bind(capture, context)
        }.onFailure { throwable ->
            Log.e(TAG, "enrolment camera binding failed", throwable)
        }
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}

/**
 * Triggers a capture and hands back an upright bitmap.
 *
 * Held by the caller so the shutter button can live anywhere in the layout rather
 * than being baked into the preview.
 */
class EnrolmentCameraController {

    private var imageCapture: ImageCapture? = null
    private var context: Context? = null

    /** Whether the camera has bound and a capture can be taken. */
    val isReady: Boolean get() = imageCapture != null

    internal fun bind(capture: ImageCapture, context: Context) {
        this.imageCapture = capture
        this.context = context
    }

    /**
     * Takes one photo.
     *
     * @return An upright bitmap, or `null` if the camera is not bound or the
     *   capture failed. Rotation is applied here so nothing downstream — detection,
     *   alignment — has to reason about sensor orientation.
     */
    suspend fun capture(): Bitmap? {
        val capture = imageCapture ?: return null
        val appContext = context ?: return null

        return runCatching { capture.takeUpright(appContext) }
            .onFailure { throwable -> Log.w(TAG, "capture failed", throwable) }
            .getOrNull()
    }
}

private suspend fun ImageCapture.takeUpright(context: Context): Bitmap =
    suspendCancellableCoroutine { continuation ->
        takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    try {
                        val upright = image.toBitmap().rotated(image.imageInfo.rotationDegrees)
                        continuation.resume(upright)
                    } catch (throwable: Throwable) {
                        continuation.resumeWithException(throwable)
                    } finally {
                        // Not closing stalls the capture pipeline after a couple
                        // of shots, which looks like the button having stopped
                        // working rather than a leak.
                        image.close()
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    continuation.resumeWithException(exception)
                }
            },
        )
    }

private fun Bitmap.rotated(degrees: Int): Bitmap {
    if (degrees == 0) return this

    val matrix = Matrix().apply { postRotate(degrees.toFloat()) }

    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

/** Bridges CameraX's `ListenableFuture` to a coroutine without a Guava dependency. */
private suspend fun Context.awaitCameraProvider(): ProcessCameraProvider =
    suspendCancellableCoroutine { continuation ->
        val future = ProcessCameraProvider.getInstance(this)

        future.addListener(
            {
                runCatching { future.get() }
                    .onSuccess { provider -> continuation.resume(provider) }
                    .onFailure { throwable -> continuation.resumeWithException(throwable) }
            },
            ContextCompat.getMainExecutor(this),
        )
    }

private const val TAG = "EnrolmentCamera"
