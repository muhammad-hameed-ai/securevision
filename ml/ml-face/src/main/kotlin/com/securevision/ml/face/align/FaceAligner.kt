package com.securevision.ml.face.align

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import com.securevision.core.model.FaceLandmarks
import com.securevision.core.model.NormalisedPoint
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Warps a detected face onto the canonical template.
 *
 * **This stage is mandatory and must never be bypassed.** The previous version of
 * this app fed raw, unaligned face crops straight to the embedder and scored
 * roughly 0.23 similarity for every face it saw — known and unknown alike. The
 * reason is that an embedding model has no way to separate "different person"
 * from "same person, head tilted twenty degrees" unless something first removes
 * the pose. That something is this class.
 *
 * The output is always [AlignmentTemplate.OUTPUT_SIZE] square with the eyes
 * horizontal, centred, and a fixed distance apart, regardless of how the subject
 * was oriented in the frame.
 */
@Singleton
class FaceAligner @Inject constructor() {

    /**
     * Produces an aligned crop from a full frame and its landmarks.
     *
     * @param frame The full, upright camera frame.
     * @param landmarks The five landmarks in normalised frame coordinates.
     * @return A square aligned crop, or `null` when the landmarks are degenerate
     *   and no transform can be recovered. Returning `null` rather than an
     *   unaligned fallback is deliberate: a silently unaligned crop is precisely
     *   the failure this class exists to prevent, and it would be indistinguishable
     *   from a working one until match scores collapsed.
     */
    fun align(frame: Bitmap, landmarks: FaceLandmarks): Bitmap? {
        val transform = solveTransform(frame.width, frame.height, landmarks) ?: return null

        val output = Bitmap.createBitmap(
            AlignmentTemplate.OUTPUT_SIZE,
            AlignmentTemplate.OUTPUT_SIZE,
            Bitmap.Config.ARGB_8888,
        )

        val matrix = Matrix().apply { setValues(transform.toMatrixValues()) }

        Canvas(output).drawBitmap(frame, matrix, FILTER_PAINT)

        return output
    }

    /**
     * Solves the transform without applying it, so the maths can be checked
     * independently of any bitmap.
     *
     * @param frameWidth Width of the source frame in pixels.
     * @param frameHeight Height of the source frame in pixels.
     * @param landmarks Landmarks in normalised frame coordinates.
     * @return The transform mapping this face onto the template, or `null` if the
     *   landmarks are degenerate.
     */
    fun solveTransform(
        frameWidth: Int,
        frameHeight: Int,
        landmarks: FaceLandmarks,
    ): SimilarityTransform? {
        val source = landmarks.asOrderedList().map { point ->
            point.toPixels(frameWidth, frameHeight)
        }

        return SimilarityTransform.solve(
            source = source,
            destination = AlignmentTemplate.orderedPoints,
        )
    }

    private fun NormalisedPoint.toPixels(width: Int, height: Int): PixelPoint =
        PixelPoint(x = x * width, y = y * height)

    private companion object {
        /**
         * Bilinear filtering on the warp.
         *
         * The crop is almost always a downscale, and nearest-neighbour sampling
         * would introduce aliasing artefacts that the embedder sees as texture
         * that is not on the person's face.
         */
        val FILTER_PAINT = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    }
}
