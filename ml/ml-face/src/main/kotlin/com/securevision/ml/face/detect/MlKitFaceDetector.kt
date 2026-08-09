package com.securevision.ml.face.detect

import android.graphics.Bitmap
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import com.securevision.core.model.BoundingBox
import com.securevision.core.model.FaceDetection
import com.securevision.core.model.FaceLandmarks
import com.securevision.core.model.NormalisedPoint
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import com.google.mlkit.vision.face.FaceDetection as MlKitFaceDetection

/**
 * Locates faces and their alignment landmarks using ML Kit.
 *
 * Configured for accuracy rather than speed, because everything downstream
 * depends on the landmark positions being right: a landmark off by a few pixels
 * tilts the alignment transform, and a tilted alignment is what degrades the
 * embedding. The pipeline is throttled at the frame level instead, which buys
 * back the cost without compromising any individual detection.
 *
 * Tracking is enabled because multi-frame voting keys on the tracking id. Without
 * it every frame would look like a new face and voting could never accumulate.
 */
@Singleton
class MlKitFaceDetector @Inject constructor() {

    private val detector by lazy {
        MlKitFaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                // Enabled for the coarse emotion signal. ML Kit computes the smile
                // score during detection, so this is the one attribute available
                // without a separate model — and without a second inference pass.
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .setMinFaceSize(MIN_RELATIVE_FACE_SIZE)
                .enableTracking()
                .build(),
        )
    }

    /**
     * Detects every face in a frame.
     *
     * @param frame An upright bitmap; rotation is resolved before this call so
     *   landmark coordinates need no further correction.
     * @return One [FaceDetection] per face, in normalised frame coordinates.
     */
    suspend fun detect(frame: Bitmap): List<FaceDetection> {
        val image = InputImage.fromBitmap(frame, UPRIGHT_ROTATION)

        val faces = runCatching { detector.process(image).await() }
            .onFailure { throwable -> Log.w(TAG, "detection failed", throwable) }
            .getOrDefault(emptyList())

        return faces.map { face -> face.toDetection(frame.width, frame.height) }
    }

    /** Releases the underlying detector. Called when the pipeline shuts down. */
    fun close() {
        runCatching { detector.close() }
            .onFailure { throwable -> Log.w(TAG, "detector close failed", throwable) }
    }

    private fun Face.toDetection(frameWidth: Int, frameHeight: Int): FaceDetection {
        val width = frameWidth.toFloat()
        val height = frameHeight.toFloat()

        return FaceDetection(
            // A detector without tracking returns null; -1 keeps such a face out
            // of the voter's per-id history rather than pooling every untracked
            // face under a shared key.
            trackingId = trackingId ?: UNTRACKED_ID,
            boundingBox = BoundingBox(
                left = boundingBox.left / width,
                top = boundingBox.top / height,
                right = boundingBox.right / width,
                bottom = boundingBox.bottom / height,
            ),
            landmarks = extractLandmarks(width, height),
            yawDegrees = headEulerAngleY,
            rollDegrees = headEulerAngleZ,
            // Null when the detector declined on this face. Passed through as null
            // rather than defaulted, so "not assessed" survives to the alert.
            smilingProbability = smilingProbability,
        )
    }

    /**
     * Collects the five alignment landmarks.
     *
     * Returns `null` unless **all five** are present. A partial set cannot be
     * fitted to the template, and substituting estimates for the missing points
     * would produce a transform that looks valid and aligns to the wrong geometry.
     */
    private fun Face.extractLandmarks(width: Float, height: Float): FaceLandmarks? {
        val leftEye = getLandmark(FaceLandmark.LEFT_EYE) ?: return null
        val rightEye = getLandmark(FaceLandmark.RIGHT_EYE) ?: return null
        val noseBase = getLandmark(FaceLandmark.NOSE_BASE) ?: return null
        val leftMouth = getLandmark(FaceLandmark.MOUTH_LEFT) ?: return null
        val rightMouth = getLandmark(FaceLandmark.MOUTH_RIGHT) ?: return null

        return FaceLandmarks(
            leftEye = NormalisedPoint(leftEye.position.x / width, leftEye.position.y / height),
            rightEye = NormalisedPoint(rightEye.position.x / width, rightEye.position.y / height),
            noseBase = NormalisedPoint(noseBase.position.x / width, noseBase.position.y / height),
            leftMouth = NormalisedPoint(leftMouth.position.x / width, leftMouth.position.y / height),
            rightMouth = NormalisedPoint(
                rightMouth.position.x / width,
                rightMouth.position.y / height,
            ),
        )
    }

    /**
     * Bridges a Play Services [Task] to a coroutine.
     *
     * Hand-written rather than pulling in `kotlinx-coroutines-play-services` for
     * one function, and cancellation-aware so a cancelled analysis does not leave
     * a continuation suspended forever.
     */
    private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { result -> continuation.resume(result) }
        addOnFailureListener { throwable -> continuation.resumeWithException(throwable) }
        addOnCanceledListener { continuation.cancel() }
    }

    private companion object {
        const val TAG = "FaceDetector"

        /** The frame is already upright by the time it reaches here. */
        const val UPRIGHT_ROTATION = 0

        /** Faces smaller than this fraction of the frame are not reported at all. */
        const val MIN_RELATIVE_FACE_SIZE = 0.10f

        /** Stands in for a face the detector could not assign a tracking id. */
        const val UNTRACKED_ID = -1
    }
}
