package com.securevision.core.model

import kotlin.math.abs

/**
 * A face located in one analysed frame, before any recognition has happened.
 *
 * The raw output of detection. Recognition turns this into a [DetectionResult] by
 * aligning, embedding, matching and voting.
 *
 * @property trackingId Identifier that stays stable across frames for the same
 *   face. It is the key multi-frame voting accumulates evidence against, so a
 *   detector without tracking would make voting meaningless.
 * @property boundingBox Face location in normalised frame coordinates.
 * @property landmarks The five alignment points, or `null` when the detector
 *   could not locate all of them — such a face cannot be aligned and therefore
 *   must not be embedded.
 * @property yawDegrees Head rotation left/right. Positive turns the face toward
 *   the image's right.
 * @property rollDegrees Head tilt. Positive tilts the face clockwise in-image.
 */
data class FaceDetection(
    val trackingId: Int,
    val boundingBox: BoundingBox,
    val landmarks: FaceLandmarks?,
    val yawDegrees: Float,
    val rollDegrees: Float,
) {
    /** Absolute yaw, which is what the quality gate actually thresholds on. */
    val absoluteYaw: Float get() = abs(yawDegrees)

    /** Absolute roll, which is what the quality gate actually thresholds on. */
    val absoluteRoll: Float get() = abs(rollDegrees)

    /** Whether this face carries the landmarks alignment needs. */
    val isAlignable: Boolean get() = landmarks != null
}
