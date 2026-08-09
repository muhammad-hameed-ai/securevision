package com.securevision.feature.live.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * The CameraX preview, with an analysis stream feeding the pipeline.
 *
 * `FILL_CENTER` is chosen deliberately and the overlay's transform is written to
 * match it — a security preview that letterboxes wastes screen, but the scale
 * maths has to account for the crop or every box drifts toward the edges.
 *
 * @param isFrontCamera Which lens to bind.
 * @param onFrame Called with each analysed frame, already upright.
 * @param modifier Modifier applied to the preview surface.
 */
@Composable
fun CameraPreview(
    isFrontCamera: Boolean,
    onFrame: (Bitmap) -> Unit,
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

    val analysisExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose { analysisExecutor.shutdown() }
    }

    LaunchedEffect(isFrontCamera) {
        runCatching {
            val provider = context.awaitCameraProvider()

            // setSurfaceProvider, not the property: CameraX 1.3 exposes only the
            // setter, and the Kotlin property form arrives in 1.4.
            val preview = Preview.Builder().build().apply {
                setSurfaceProvider(previewView.surfaceProvider)
            }

            val analysis = ImageAnalysis.Builder()
                // Dropping stale frames rather than queueing them is what keeps
                // the overlay tracking the face instead of trailing behind it.
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .apply {
                    setAnalyzer(analysisExecutor) { imageProxy ->
                        imageProxy.useUpright(onFrame)
                    }
                }

            val selector = if (isFrontCamera) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }

            provider.unbindAll()
            provider.bindToLifecycle(lifecycleOwner, selector, preview, analysis)
        }.onFailure { throwable ->
            Log.e(TAG, "camera binding failed", throwable)
        }
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}

/**
 * Hands an upright bitmap to [block] and always closes the proxy.
 *
 * Failing to close an [ImageProxy] stalls the analyser after a couple of frames,
 * which looks exactly like the pipeline being slow rather than a leak.
 */
private inline fun ImageProxy.useUpright(block: (Bitmap) -> Unit) {
    try {
        val upright = toBitmap().rotated(imageInfo.rotationDegrees)
        block(upright)
    } catch (throwable: Throwable) {
        Log.w(TAG, "frame conversion failed", throwable)
    } finally {
        close()
    }
}

/**
 * Rotates a frame into display orientation.
 *
 * Done once here so nothing downstream — detection, alignment, the overlay — has
 * to reason about sensor orientation. Getting this wrong rotates every landmark
 * and produces an alignment transform that is confidently, uniformly wrong.
 */
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

private const val TAG = "CameraPreview"
