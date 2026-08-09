package com.securevision.ml.face.match

import com.securevision.core.model.FaceDetection
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Decides whether a detected face is worth embedding.
 *
 * Runs before alignment and inference, not after, because rejecting a face costs
 * three comparisons while embedding one costs a full model pass. More
 * importantly, a face that is tiny, turned away or heavily tilted produces an
 * embedding that is genuinely poor rather than merely noisy — passing it through
 * would let a bad crop score against an enrolled profile and pollute voting with
 * a verdict that never deserved a say.
 */
@Singleton
class FaceQualityGate @Inject constructor() {

    /**
     * Assesses one detection.
     *
     * @param detection The face to assess.
     * @return [FaceQuality.Acceptable], or the first reason it was rejected.
     */
    fun assess(detection: FaceDetection): FaceQuality = when {
        !detection.isAlignable -> FaceQuality.Rejected(FaceQuality.Reason.NO_LANDMARKS)

        detection.boundingBox.width < MIN_RELATIVE_WIDTH ->
            FaceQuality.Rejected(FaceQuality.Reason.TOO_SMALL)

        detection.absoluteYaw > MAX_YAW_DEGREES ->
            FaceQuality.Rejected(FaceQuality.Reason.TURNED_AWAY)

        detection.absoluteRoll > MAX_ROLL_DEGREES ->
            FaceQuality.Rejected(FaceQuality.Reason.TOO_TILTED)

        else -> FaceQuality.Acceptable
    }

    companion object {
        /**
         * Minimum face width as a fraction of frame width.
         *
         * Below this the crop upscales to 160×160 from too few real pixels, and
         * the embedder is reading interpolation rather than a face.
         */
        const val MIN_RELATIVE_WIDTH = 0.10f

        /**
         * Maximum absolute yaw in degrees.
         *
         * Alignment corrects roll well and yaw only partially: past roughly this
         * angle one side of the face is genuinely not visible, and no 2-D warp can
         * recover what the camera never saw.
         */
        const val MAX_YAW_DEGREES = 35f

        /** Maximum absolute roll in degrees. */
        const val MAX_ROLL_DEGREES = 30f
    }
}

/** Whether a face is usable for recognition. */
sealed interface FaceQuality {

    /** The face can be aligned and embedded. */
    data object Acceptable : FaceQuality

    /**
     * The face was rejected.
     *
     * @property reason Why, so the live overlay can hint at what to change.
     */
    data class Rejected(val reason: Reason) : FaceQuality

    /** Why a face failed the gate. */
    enum class Reason {
        /** The detector could not locate all five alignment landmarks. */
        NO_LANDMARKS,

        /** Too few pixels on the face to embed meaningfully. */
        TOO_SMALL,

        /** Head turned too far for alignment to recover. */
        TURNED_AWAY,

        /** Head tilted beyond what the gate accepts. */
        TOO_TILTED,
    }
}
