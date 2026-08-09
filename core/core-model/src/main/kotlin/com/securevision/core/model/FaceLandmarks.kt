package com.securevision.core.model

/**
 * A point in **normalised frame coordinates**, where `0f` is the left/top edge of
 * the analysed frame and `1f` is the right/bottom edge.
 *
 * Normalised rather than pixels so landmarks survive being passed between the
 * analysis resolution, the alignment crop and the preview surface without any of
 * them needing to know the others' dimensions.
 *
 * @property x Horizontal position, normally in `0f..1f`.
 * @property y Vertical position, normally in `0f..1f`.
 */
data class NormalisedPoint(val x: Float, val y: Float)

/**
 * The five facial landmarks alignment requires.
 *
 * Exactly these five, in this order, because they are the points the canonical
 * reference template is defined against. Alignment solves a similarity transform
 * mapping these onto that template — which is the stage whose absence produced a
 * similarity of roughly 0.23 for every face in the previous version of this app.
 *
 * @property leftEye Centre of the subject's left eye, as seen in the image.
 * @property rightEye Centre of the subject's right eye, as seen in the image.
 * @property noseBase Base of the nose.
 * @property leftMouth Left corner of the mouth.
 * @property rightMouth Right corner of the mouth.
 */
data class FaceLandmarks(
    val leftEye: NormalisedPoint,
    val rightEye: NormalisedPoint,
    val noseBase: NormalisedPoint,
    val leftMouth: NormalisedPoint,
    val rightMouth: NormalisedPoint,
) {
    /** The five points in the fixed order the alignment template expects. */
    fun asOrderedList(): List<NormalisedPoint> =
        listOf(leftEye, rightEye, noseBase, leftMouth, rightMouth)

    companion object {
        /** How many points alignment needs. A face missing any of them cannot be aligned. */
        const val REQUIRED_POINT_COUNT: Int = 5
    }
}
