package com.securevision.ml.face.align

import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * A 2-D similarity transform: uniform scale, rotation and translation.
 *
 * ```
 * x' = a·x − b·y + tx
 * y' = b·x + a·y + ty
 * ```
 *
 * Four parameters, deliberately not six. A full affine transform would also
 * allow shear and non-uniform scale, which would stretch a face into the
 * template's proportions rather than rotating and scaling it into place — the
 * embedder was trained on faces, not on faces squashed to fit.
 *
 * @property a Cosine component, scaled.
 * @property b Sine component, scaled.
 * @property tx Horizontal translation.
 * @property ty Vertical translation.
 */
data class SimilarityTransform(
    val a: Float,
    val b: Float,
    val tx: Float,
    val ty: Float,
) {

    /** Uniform scale factor applied by this transform. */
    val scale: Float get() = sqrt(a * a + b * b)

    /** Rotation in degrees, positive counter-clockwise in image coordinates. */
    val rotationDegrees: Float
        get() = Math.toDegrees(atan2(b.toDouble(), a.toDouble())).toFloat()

    /**
     * Maps a point through this transform.
     *
     * @param point Point in source space.
     * @return The corresponding point in destination space.
     */
    fun apply(point: PixelPoint): PixelPoint = PixelPoint(
        x = a * point.x - b * point.y + tx,
        y = b * point.x + a * point.y + ty,
    )

    /**
     * The nine values `android.graphics.Matrix` expects, row-major.
     *
     * Kept here so the maths stays in one testable place and the Android type is
     * only constructed at the point of use.
     */
    fun toMatrixValues(): FloatArray = floatArrayOf(
        a, -b, tx,
        b, a, ty,
        0f, 0f, 1f,
    )

    companion object {

        /**
         * Least-squares fit of the similarity transform mapping [source] onto
         * [destination].
         *
         * Solved in closed form rather than by SVD. Because the transform is
         * linear in its four parameters, the normal equations reduce to two ratios
         * — exact, allocation-free, and with no iteration to converge or fail to
         * converge on a degenerate frame. This is the same fit the Umeyama
         * algorithm produces for the no-reflection case.
         *
         * @param source Detected landmark positions.
         * @param destination Template positions, in the same order.
         * @return The best-fit transform, or `null` when the source points are
         *   effectively coincident and no scale can be recovered.
         * @throws IllegalArgumentException if the two lists differ in length or
         *   hold fewer than two points.
         */
        fun solve(
            source: List<PixelPoint>,
            destination: List<PixelPoint>,
        ): SimilarityTransform? {
            require(source.size == destination.size) {
                "point counts differ: ${source.size} vs ${destination.size}"
            }
            require(source.size >= MIN_POINTS) {
                "a similarity transform needs at least $MIN_POINTS points, got ${source.size}"
            }

            val count = source.size
            val sourceMeanX = source.sumOf { it.x.toDouble() } / count
            val sourceMeanY = source.sumOf { it.y.toDouble() } / count
            val destMeanX = destination.sumOf { it.x.toDouble() } / count
            val destMeanY = destination.sumOf { it.y.toDouble() } / count

            var sourceVariance = 0.0
            var dotProduct = 0.0
            var crossProduct = 0.0

            for (index in 0 until count) {
                val sx = source[index].x - sourceMeanX
                val sy = source[index].y - sourceMeanY
                val dx = destination[index].x - destMeanX
                val dy = destination[index].y - destMeanY

                sourceVariance += sx * sx + sy * sy
                dotProduct += sx * dx + sy * dy
                crossProduct += sx * dy - sy * dx
            }

            // Every source point sits on top of the mean: there is no orientation
            // or scale to recover, and dividing would produce infinities that would
            // propagate silently into the warp.
            if (sourceVariance < DEGENERATE_VARIANCE) return null

            val a = dotProduct / sourceVariance
            val b = crossProduct / sourceVariance

            return SimilarityTransform(
                a = a.toFloat(),
                b = b.toFloat(),
                tx = (destMeanX - (a * sourceMeanX - b * sourceMeanY)).toFloat(),
                ty = (destMeanY - (b * sourceMeanX + a * sourceMeanY)).toFloat(),
            )
        }

        /** Two points are the minimum that determine a similarity transform. */
        const val MIN_POINTS: Int = 2

        /** Below this total variance the source points are treated as coincident. */
        private const val DEGENERATE_VARIANCE: Double = 1e-6
    }
}
