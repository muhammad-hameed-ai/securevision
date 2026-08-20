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
    fun assess(detection: FaceDetection, frameAspect: Float = SQUARE_ASPECT): FaceQuality = when {
        !detection.isAlignable -> FaceQuality.Rejected(FaceQuality.Reason.NO_LANDMARKS)

        detection.relativeWidthOnShortEdge(frameAspect) < MIN_RELATIVE_WIDTH ->
            FaceQuality.Rejected(FaceQuality.Reason.TOO_SMALL)

        detection.absoluteYaw > MAX_YAW_DEGREES ->
            FaceQuality.Rejected(FaceQuality.Reason.TURNED_AWAY)

        detection.absoluteRoll > MAX_ROLL_DEGREES ->
            FaceQuality.Rejected(FaceQuality.Reason.TOO_TILTED)

        else -> FaceQuality.Acceptable
    }

    /**
     * Face width as a fraction of the frame's **shorter** edge.
     *
     * The bounding box is normalised against frame width, which makes it
     * orientation-dependent: the same physical face is a smaller fraction of a
     * 640-wide landscape frame than of a 480-wide portrait one. Measuring
     * against the short edge instead gives the same number in both, so a face
     * accepted in portrait is not silently rejected the moment the phone turns.
     *
     * That silent rejection was a real defect: a rejected face rendered
     * identically to one still being resolved, which in turn rendered as green.
     *
     * @param frameAspect Frame width divided by height.
     */
    private fun FaceDetection.relativeWidthOnShortEdge(frameAspect: Float): Float =
        if (frameAspect > 1f) boundingBox.width * frameAspect else boundingBox.width

    companion object {
        /** A frame whose edges are equal, where the correction is a no-op. */
        const val SQUARE_ASPECT = 1f

        /**
         * Minimum face width as a fraction of the frame's shorter edge.
         *
         * Below this the crop upscales to 160×160 from too few real pixels, and
         * the embedder is reading interpolation rather than a face.
         *
         * This number is a direct trade between range and false positives, and it
         * has moved twice for good reasons. 0.10 let hands and fragments of
         * objects draw boxes. 0.18 stopped that but also refused genuinely
         * distant faces. 0.13 sits between: roughly 62 px on a 480-tall analysis
         * frame, which is enough pixels for a usable embedding while staying well
         * above the small spurious candidates ML Kit offers.
         *
         * What actually holds the line against hands and objects is the
         * five-landmark requirement, not this floor — an object has no eyes or
         * mouth at any size. Lower this further only with that in mind.
         */
        const val MIN_RELATIVE_WIDTH = 0.13f

        /**
         * Maximum absolute yaw in degrees.
         *
         * Alignment corrects roll well and yaw only partially: past roughly this
         * angle one side of the face is genuinely not visible, and no 2-D warp can
         * recover what the camera never saw.
         */
        const val MAX_YAW_DEGREES = 35f

        /**
         * Maximum absolute roll in degrees.
         *
         * Raised from 30 because rotating the phone shifts a standing person's
         * apparent roll, and a valid landscape face was being rejected as
         * TOO_TILTED. 30 was never easy to justify anyway: alignment's whole job
         * is removing roll, so gating tightly on it discards faces the aligner
         * would have squared up perfectly well.
         *
         * If a face still fails in landscape with roll near 90, the frame itself
         * is arriving rotated and no limit below 90 will help — that is an
         * orientation problem, which is why the gate now logs what it saw.
         */
        const val MAX_ROLL_DEGREES = 45f
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
