package com.securevision.ml.face.align

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The alignment maths, tested on synthetic point sets.
 *
 * This is the closest a unit test can get to the defect that broke the previous
 * app: if the transform is wrong, alignment silently produces a plausible-looking
 * crop with the wrong geometry, and every embedding downstream is degraded
 * without anything appearing to fail.
 */
class SimilarityTransformTest {

    @Test
    fun `recovers the identity transform from unchanged points`() {
        val points = squarePoints()

        val transform = SimilarityTransform.solve(points, points)

        assertNotNull(transform)
        transform!!
        assertEquals(1f, transform.scale, TOLERANCE)
        assertEquals(0f, transform.rotationDegrees, TOLERANCE)
        assertEquals(0f, transform.tx, TOLERANCE)
        assertEquals(0f, transform.ty, TOLERANCE)
    }

    @Test
    fun `recovers a pure translation`() {
        val source = squarePoints()
        val destination = source.map { PixelPoint(it.x + 25f, it.y - 40f) }

        val transform = SimilarityTransform.solve(source, destination)!!

        assertEquals(1f, transform.scale, TOLERANCE)
        assertEquals(0f, transform.rotationDegrees, TOLERANCE)
        assertEquals(25f, transform.tx, TOLERANCE)
        assertEquals(-40f, transform.ty, TOLERANCE)
    }

    @Test
    fun `recovers a pure uniform scale`() {
        val source = squarePoints()
        val destination = source.map { PixelPoint(it.x * 2.5f, it.y * 2.5f) }

        val transform = SimilarityTransform.solve(source, destination)!!

        assertEquals(2.5f, transform.scale, TOLERANCE)
        assertEquals(0f, transform.rotationDegrees, TOLERANCE)
    }

    @Test
    fun `recovers a known rotation`() {
        val degrees = 30.0
        val source = squarePoints()
        val destination = source.map { it.rotatedBy(degrees) }

        val transform = SimilarityTransform.solve(source, destination)!!

        assertEquals(30f, transform.rotationDegrees, ANGLE_TOLERANCE)
        assertEquals(1f, transform.scale, TOLERANCE)
    }

    @Test
    fun `recovers a combined rotation, scale and translation`() {
        val degrees = -22.0
        val scale = 1.8f
        val source = squarePoints()
        val destination = source.map { point ->
            val rotated = point.rotatedBy(degrees)
            PixelPoint(rotated.x * scale + 12f, rotated.y * scale - 7f)
        }

        val transform = SimilarityTransform.solve(source, destination)!!

        assertEquals(-22f, transform.rotationDegrees, ANGLE_TOLERANCE)
        assertEquals(scale, transform.scale, TOLERANCE)

        // The real assertion: applying the recovered transform reproduces the
        // destination points. Parameters matching is nice; the mapping being right
        // is what alignment actually depends on.
        source.forEachIndexed { index, point ->
            val mapped = transform.apply(point)
            assertEquals(destination[index].x, mapped.x, MAPPING_TOLERANCE)
            assertEquals(destination[index].y, mapped.y, MAPPING_TOLERANCE)
        }
    }

    @Test
    fun `maps real landmark geometry onto the template`() {
        // A face rolled 15 degrees and half the template's size, offset in frame.
        val source = AlignmentTemplate.orderedPoints.map { point ->
            val rotated = point.rotatedBy(15.0)
            PixelPoint(rotated.x * 0.5f + 200f, rotated.y * 0.5f + 120f)
        }

        val transform = SimilarityTransform.solve(source, AlignmentTemplate.orderedPoints)!!

        source.forEachIndexed { index, point ->
            val mapped = transform.apply(point)
            val expected = AlignmentTemplate.orderedPoints[index]
            assertEquals(expected.x, mapped.x, MAPPING_TOLERANCE)
            assertEquals(expected.y, mapped.y, MAPPING_TOLERANCE)
        }
    }

    @Test
    fun `removes roll, landing the eyes on the template's own orientation`() {
        val rolled = AlignmentTemplate.orderedPoints.map { it.rotatedBy(25.0) }

        val transform = SimilarityTransform.solve(rolled, AlignmentTemplate.orderedPoints)!!

        val leftEye = transform.apply(rolled[0])
        val rightEye = transform.apply(rolled[1])

        assertEquals(AlignmentTemplate.leftEye.x, leftEye.x, MAPPING_TOLERANCE)
        assertEquals(AlignmentTemplate.leftEye.y, leftEye.y, MAPPING_TOLERANCE)
        assertEquals(AlignmentTemplate.rightEye.x, rightEye.x, MAPPING_TOLERANCE)
        assertEquals(AlignmentTemplate.rightEye.y, rightEye.y, MAPPING_TOLERANCE)

        // The target is the template's tilt, not zero: the ArcFace reference eyes
        // differ by roughly 0.28 px at this scale, so asserting perfectly level
        // eyes would be asserting something the template itself does not satisfy.
        val alignedTilt = tiltDegrees(leftEye, rightEye)
        val templateTilt = tiltDegrees(AlignmentTemplate.leftEye, AlignmentTemplate.rightEye)

        assertEquals(templateTilt, alignedTilt, ANGLE_TOLERANCE)

        // The point of the stage: 25 degrees in, essentially none out.
        assertTrue("roll should be removed, tilt was $alignedTilt", abs(alignedTilt) < 1.0)
        assertTrue("eyes should be separated horizontally", rightEye.x - leftEye.x > 0f)
    }

    @Test
    fun `puts the eyes at the template's inter-ocular distance regardless of face size`() {
        // A face captured at a quarter of the template's scale still has to come
        // out the same size, or the embedder sees a different amount of face.
        val small = AlignmentTemplate.orderedPoints.map { PixelPoint(it.x * 0.25f, it.y * 0.25f) }

        val transform = SimilarityTransform.solve(small, AlignmentTemplate.orderedPoints)!!

        val leftEye = transform.apply(small[0])
        val rightEye = transform.apply(small[1])

        assertEquals(
            AlignmentTemplate.interocularDistance,
            rightEye.x - leftEye.x,
            MAPPING_TOLERANCE,
        )
    }

    @Test
    fun `introduces no shear, so a square stays square`() {
        val transform = SimilarityTransform.solve(
            squarePoints(),
            squarePoints().map { it.rotatedBy(40.0) },
        )!!

        val corners = squarePoints().map(transform::apply)
        val topEdge = corners[0].distanceTo(corners[1])
        val rightEdge = corners[1].distanceTo(corners[2])

        // A full affine fit would allow these to diverge, stretching the face into
        // the template rather than rotating it into place.
        assertEquals(topEdge, rightEdge, MAPPING_TOLERANCE)
    }

    @Test
    fun `returns null for coincident source points`() {
        val coincident = List(5) { PixelPoint(50f, 50f) }

        assertNull(SimilarityTransform.solve(coincident, AlignmentTemplate.orderedPoints))
    }

    @Test
    fun `rejects mismatched point counts`() {
        val failure = assertThrows(IllegalArgumentException::class.java) {
            SimilarityTransform.solve(squarePoints(), squarePoints().take(3))
        }

        assertTrue(failure.message!!.contains("point counts differ"))
    }

    @Test
    fun `rejects fewer than two points`() {
        val single = listOf(PixelPoint(1f, 1f))

        assertThrows(IllegalArgumentException::class.java) {
            SimilarityTransform.solve(single, single)
        }
    }

    @Test
    fun `matrix values are laid out row-major for android Matrix`() {
        val transform = SimilarityTransform(a = 2f, b = 3f, tx = 4f, ty = 5f)

        assertTrue(
            transform.toMatrixValues().contentEquals(
                floatArrayOf(2f, -3f, 4f, 3f, 2f, 5f, 0f, 0f, 1f),
            ),
        )
    }

    private fun squarePoints() = listOf(
        PixelPoint(0f, 0f),
        PixelPoint(10f, 0f),
        PixelPoint(10f, 10f),
        PixelPoint(0f, 10f),
        PixelPoint(5f, 5f),
    )

    private fun PixelPoint.rotatedBy(degrees: Double): PixelPoint {
        val radians = Math.toRadians(degrees)
        val cosine = cos(radians).toFloat()
        val sine = sin(radians).toFloat()

        return PixelPoint(x = x * cosine - y * sine, y = x * sine + y * cosine)
    }

    private fun PixelPoint.distanceTo(other: PixelPoint): Float {
        val dx = x - other.x
        val dy = y - other.y
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }

    /** Angle of the line between two points, in degrees. */
    private fun tiltDegrees(from: PixelPoint, to: PixelPoint): Float =
        Math.toDegrees(atan2((to.y - from.y).toDouble(), (to.x - from.x).toDouble())).toFloat()

    private companion object {
        const val TOLERANCE = 1e-3f
        const val ANGLE_TOLERANCE = 1e-2f
        const val MAPPING_TOLERANCE = 1e-2f
    }
}
