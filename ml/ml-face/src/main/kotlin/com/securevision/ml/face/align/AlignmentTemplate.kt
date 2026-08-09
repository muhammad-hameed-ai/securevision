package com.securevision.ml.face.align

/**
 * The canonical five-point face template every crop is warped onto.
 *
 * These are the positions the eyes, nose and mouth corners are expected to
 * occupy in the model's input. Alignment solves a transform that puts the
 * detected landmarks here, which is what makes two photographs of the same
 * person — at different angles, distances and tilts — produce comparable
 * embeddings.
 *
 * The values are the widely used ArcFace/InsightFace reference points, defined
 * for a 112×112 crop and scaled to this pipeline's [OUTPUT_SIZE]. They are not
 * arbitrary: face embedding models are trained on crops aligned to approximately
 * this geometry, so departing from it degrades accuracy even though nothing
 * visibly breaks.
 */
object AlignmentTemplate {

    /** Edge length of the aligned crop the embedder consumes. */
    const val OUTPUT_SIZE: Int = 160

    /** Edge length the reference points were originally defined for. */
    private const val REFERENCE_SIZE: Float = 112f

    private const val SCALE: Float = OUTPUT_SIZE / REFERENCE_SIZE

    /** Subject's left eye. */
    val leftEye = PixelPoint(38.2946f * SCALE, 51.6963f * SCALE)

    /** Subject's right eye. */
    val rightEye = PixelPoint(73.5318f * SCALE, 51.5014f * SCALE)

    /** Nose base. */
    val noseBase = PixelPoint(56.0252f * SCALE, 71.7366f * SCALE)

    /** Left mouth corner. */
    val leftMouth = PixelPoint(41.5493f * SCALE, 92.3655f * SCALE)

    /** Right mouth corner. */
    val rightMouth = PixelPoint(70.7299f * SCALE, 92.2041f * SCALE)

    /**
     * The five points in the same order as
     * [com.securevision.core.model.FaceLandmarks.asOrderedList].
     *
     * Order is load-bearing: the transform pairs source and destination by index,
     * so a mismatch here maps eyes onto mouth corners and produces a warp that is
     * geometrically valid and completely wrong.
     */
    val orderedPoints: List<PixelPoint> =
        listOf(leftEye, rightEye, noseBase, leftMouth, rightMouth)

    /** Distance between the template's eyes, useful for asserting an alignment worked. */
    val interocularDistance: Float = rightEye.x - leftEye.x
}
