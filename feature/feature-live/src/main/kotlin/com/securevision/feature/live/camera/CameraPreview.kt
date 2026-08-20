package com.securevision.feature.live.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import android.util.Size
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
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
import kotlin.math.roundToInt
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
    recorder: VideoRecorder? = null,
    onBindResult: (BindOutcome) -> Unit = {},
    onCameraReady: (Camera?, Boolean) -> Unit = { _, _ -> },
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

    // Built once and reused across lens changes. Rebuilding them per flip
    // reallocated the analysis buffers every time, which was the bulk of the
    // delay the user feels when switching cameras.
    val preview = remember {
        // setSurfaceProvider, not the property: CameraX 1.3 exposes only the
        // setter, and the Kotlin property form arrives in 1.4.
        Preview.Builder()
            // Pinned to the same 4:3 the analysis stream uses. Left unset, CameraX
            // sizes the preview from the display — on a tall modern phone that is
            // roughly 20:9 — while analysis stays 4:3. The two streams then show
            // different fields of view, and the overlay, which maps boxes using
            // the *analysis* dimensions, drew every box shifted vertically:
            // sitting above the face with the chin cut off.
            //
            // Matching the ratios fixes it at the source. Patching an offset into
            // OverlayTransform would have worked on this device and been wrong on
            // the next one.
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .build()
            .apply { setSurfaceProvider(previewView.surfaceProvider) }
    }

    val analysis = remember {
        ImageAnalysis.Builder()
            // Dropping stale frames rather than queueing them is what keeps the
            // overlay tracking the face instead of trailing behind it.
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            // Left unset, CameraX defaults the analysis stream to 640×480. That
            // ceiling is the single biggest limit on recognising a face across a
            // room: at 480 lines a person a few metres away is a few dozen pixels
            // tall, and no amount of upscaling recovers detail the sensor never
            // sampled. Asking for 720p roughly doubles the pixels on a distant
            // face. It is a resolution *request* — CameraX picks the nearest size
            // the hardware actually supports.
            .setTargetResolution(ANALYSIS_RESOLUTION)
            .build()
    }

    // The analyzer closes over `onFrame`, which changes identity on recomposition.
    // Re-setting it is cheap; rebuilding the use case is not.
    DisposableEffect(analysis, onFrame) {
        analysis.setAnalyzer(analysisExecutor) { imageProxy -> imageProxy.useUpright(onFrame) }
        onDispose { analysis.clearAnalyzer() }
    }

    DisposableEffect(Unit) {
        onDispose { analysisExecutor.shutdown() }
    }

    LaunchedEffect(isFrontCamera) {
        Log.i(FLIP_TAG, "rebind start: front=$isFrontCamera")

        runCatching {
            val provider = context.awaitCameraProvider()

            val selector = if (isFrontCamera) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }

            provider.unbindAll()

            val result = bindBestEffort(
                provider = provider,
                lifecycleOwner = lifecycleOwner,
                selector = selector,
                preview = preview,
                analysis = analysis,
                videoCapture = recorder?.videoCapture,
            )

            result.camera?.brightenForIndoorUse()

            // Handed upward so the HUD can offer a torch — and only offer it on a
            // camera that actually has one. Most front cameras do not, and a
            // control that silently does nothing is worse than no control.
            onCameraReady(result.camera, result.camera?.cameraInfo?.hasFlashUnit() == true)

            Log.i(FLIP_TAG, "rebind success: front=$isFrontCamera outcome=${result.outcome}")
            onBindResult(result.outcome)
        }.onFailure { throwable ->
            Log.e(FLIP_TAG, "rebind FAILED: front=$isFrontCamera", throwable)
            onBindResult(BindOutcome.PREVIEW_ONLY)
            onCameraReady(null, false)
        }
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}

/**
 * What the camera actually managed to bind.
 *
 * Not every device can run preview, image analysis and video capture at once —
 * CameraX guarantees the combination only above the LEGACY hardware level. Rather
 * than let `bindToLifecycle` throw and leave a black screen, the caller degrades
 * in a defined order and reports which rung it landed on.
 */
enum class BindOutcome {

    /** Everything bound. Detection and recording run together. */
    FULL,

    /**
     * Video bound, analysis did not.
     *
     * Detection is paused for the duration of the recording. The screen says so:
     * a security app that quietly stops detecting is worse than one that cannot
     * record at all.
     */
    VIDEO_WITHOUT_ANALYSIS,

    /** Analysis bound, video did not. Detection runs; recording is unavailable. */
    ANALYSIS_ONLY,

    /** Only the preview survived. Neither detection nor recording is available. */
    PREVIEW_ONLY,
}

/**
 * What a bind attempt produced.
 *
 * @property outcome Which rung of the fallback ladder succeeded.
 * @property camera The bound camera, for post-bind control such as exposure.
 */
private data class BindResult(val outcome: BindOutcome, val camera: Camera?)

/**
 * Nudges exposure up for indoor monitoring.
 *
 * The default auto-exposure targets a middle grey across the whole frame, which
 * indoors — with a bright window or lamp in shot — leaves faces underexposed. That
 * is not only unpleasant to look at: the same frames feed detection and the
 * embedder, and a dark crop produces a weaker embedding and worse matching.
 *
 * The step is expressed in the device's own compensation units and clamped to the
 * range it reports, because those units differ per sensor and an out-of-range
 * index is rejected outright.
 */
private fun Camera.brightenForIndoorUse() {
    val state = cameraInfo.exposureState

    if (!state.isExposureCompensationSupported) {
        Log.i(TAG, "exposure compensation unsupported on this camera")
        return
    }

    val step = state.exposureCompensationStep.toFloat()
    if (step <= 0f) return

    val desiredIndex = (INDOOR_EXPOSURE_STOPS / step).roundToInt()
    val index = desiredIndex.coerceIn(
        state.exposureCompensationRange.lower,
        state.exposureCompensationRange.upper,
    )

    cameraControl.setExposureCompensationIndex(index)
    Log.i(TAG, "exposure compensation set to index $index (step $step EV)")
}

/**
 * Binds as much as this device will accept, in order of what matters most.
 *
 * Detection outranks recording: this is a monitoring app, so a device that can
 * only do one keeps detecting. Recording is offered only once analysis is safe.
 */
private fun bindBestEffort(
    provider: ProcessCameraProvider,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    selector: CameraSelector,
    preview: Preview,
    analysis: ImageAnalysis,
    videoCapture: VideoCapture<Recorder>?,
): BindResult {
    if (videoCapture != null) {
        val camera = runCatching {
            provider.bindToLifecycle(lifecycleOwner, selector, preview, analysis, videoCapture)
        }.getOrNull()

        if (camera != null) return BindResult(BindOutcome.FULL, camera)

        Log.w(TAG, "device cannot bind preview + analysis + video; dropping video")
        provider.unbindAll()
    }

    val withAnalysis = runCatching {
        provider.bindToLifecycle(lifecycleOwner, selector, preview, analysis)
    }.getOrNull()

    if (withAnalysis != null) return BindResult(BindOutcome.ANALYSIS_ONLY, withAnalysis)

    Log.w(TAG, "device cannot bind analysis; preview only")
    provider.unbindAll()

    val previewOnly = runCatching { provider.bindToLifecycle(lifecycleOwner, selector, preview) }
        .onFailure { throwable -> Log.e(TAG, "preview binding failed", throwable) }
        .getOrNull()

    return BindResult(BindOutcome.PREVIEW_ONLY, previewOnly)
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

/**
 * Exposure lift for indoor monitoring, in EV stops.
 *
 * Two thirds of a stop: enough to bring a face out of shadow, small enough not to
 * blow out a window behind them.
 */
private const val INDOOR_EXPOSURE_STOPS = 0.67f

/**
 * Requested size for the analysis stream.
 *
 * Back to 640×480 after 720p made the live screen lag. Three times the pixels
 * flowed through every stage — ML Kit detection, the letterbox copy, the
 * aligner's warp and the weapon model's preprocessing — which stretched a cycle
 * from roughly 350 ms to well over a second and dragged weapon detection out to
 * several seconds with it.
 *
 * It bought perhaps a metre of recognition range and cost the frame rate. On a
 * live monitoring screen that is the wrong trade: the preview is what the
 * operator actually watches.
 */
private val ANALYSIS_RESOLUTION = Size(640, 480)

private const val TAG = "CameraPreview"

/** Shared with the ViewModel so one logcat filter shows the whole flip path. */
private const val FLIP_TAG = "CameraFlip"
