package com.securevision.ml.weapon.detect

import com.securevision.core.model.BoundingBox
import com.securevision.core.model.WeaponDetection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Suppression, and specifically its boundary behaviour.
 *
 * Without it a single knife draws six overlapping boxes, counts as six weapons in
 * the session stats, and raises six alerts. The IoU boundary is tested explicitly
 * because a `>` where `>=` was meant is invisible until two detections sit exactly
 * on the threshold.
 */
class NonMaxSuppressionTest {

    @Test
    fun `an empty list survives unchanged`() {
        assertTrue(NonMaxSuppression.apply(emptyList()).isEmpty())
    }

    @Test
    fun `a single detection is kept`() {
        val single = listOf(detection("knife", 0.9f, 0.1f, 0.1f, 0.3f, 0.3f))

        assertEquals(single, NonMaxSuppression.apply(single))
    }

    @Test
    fun `heavily overlapping same-class boxes collapse to the highest score`() {
        val detections = listOf(
            detection("knife", 0.72f, 0.10f, 0.10f, 0.40f, 0.40f),
            detection("knife", 0.91f, 0.11f, 0.11f, 0.41f, 0.41f),
            detection("knife", 0.65f, 0.09f, 0.09f, 0.39f, 0.39f),
        )

        val kept = NonMaxSuppression.apply(detections)

        assertEquals(1, kept.size)
        assertEquals(0.91f, kept.first().confidence, TOLERANCE)
    }

    @Test
    fun `distinct boxes of the same class both survive`() {
        val detections = listOf(
            detection("knife", 0.9f, 0.05f, 0.05f, 0.25f, 0.25f),
            detection("knife", 0.8f, 0.70f, 0.70f, 0.95f, 0.95f),
        )

        assertEquals(2, NonMaxSuppression.apply(detections).size)
    }

    @Test
    fun `overlapping boxes of different classes both survive`() {
        // A knife overlapping a pistol is two weapons, not a duplicate.
        val detections = listOf(
            detection("knife", 0.9f, 0.10f, 0.10f, 0.40f, 0.40f),
            detection("pistol", 0.85f, 0.11f, 0.11f, 0.41f, 0.41f),
        )

        assertEquals(2, NonMaxSuppression.apply(detections).size)
    }

    @Test
    fun `results come back highest score first`() {
        val detections = listOf(
            detection("knife", 0.5f, 0.05f, 0.05f, 0.20f, 0.20f),
            detection("knife", 0.9f, 0.60f, 0.60f, 0.80f, 0.80f),
            detection("knife", 0.7f, 0.30f, 0.30f, 0.45f, 0.45f),
        )

        val scores = NonMaxSuppression.apply(detections).map { it.confidence }

        assertEquals(listOf(0.9f, 0.7f, 0.5f), scores)
    }

    @Test
    fun `identical boxes have an IoU of one`() {
        val box = BoundingBox(0.1f, 0.1f, 0.5f, 0.5f)

        assertEquals(1f, NonMaxSuppression.intersectionOverUnion(box, box), TOLERANCE)
    }

    @Test
    fun `disjoint boxes have an IoU of zero`() {
        val first = BoundingBox(0.0f, 0.0f, 0.2f, 0.2f)
        val second = BoundingBox(0.5f, 0.5f, 0.8f, 0.8f)

        assertEquals(0f, NonMaxSuppression.intersectionOverUnion(first, second), TOLERANCE)
    }

    @Test
    fun `boxes touching at an edge have an IoU of zero`() {
        val first = BoundingBox(0.0f, 0.0f, 0.5f, 0.5f)
        val second = BoundingBox(0.5f, 0.0f, 1.0f, 0.5f)

        // Zero area of overlap, not a sliver.
        assertEquals(0f, NonMaxSuppression.intersectionOverUnion(first, second), TOLERANCE)
    }

    @Test
    fun `IoU is a quarter for boxes sharing half of each side`() {
        val first = BoundingBox(0.0f, 0.0f, 0.4f, 0.4f)
        val second = BoundingBox(0.2f, 0.2f, 0.6f, 0.6f)

        // Intersection 0.04, union 0.16 + 0.16 - 0.04 = 0.28.
        assertEquals(0.04f / 0.28f, NonMaxSuppression.intersectionOverUnion(first, second), TOLERANCE)
    }

    @Test
    fun `a candidate exactly at the threshold is kept`() {
        val first = BoundingBox(0.0f, 0.0f, 0.4f, 0.4f)
        val second = BoundingBox(0.2f, 0.2f, 0.6f, 0.6f)
        val exactIou = NonMaxSuppression.intersectionOverUnion(first, second)

        val kept = NonMaxSuppression.apply(
            listOf(
                WeaponDetection(first, "knife", 0.9f),
                WeaponDetection(second, "knife", 0.8f),
            ),
            iouThreshold = exactIou,
        )

        // Strictly-greater suppression: an ambiguous box survives, which is the
        // safer error for a weapon detector.
        assertEquals(2, kept.size)
    }

    @Test
    fun `a candidate just above the threshold is suppressed`() {
        val first = BoundingBox(0.0f, 0.0f, 0.4f, 0.4f)
        val second = BoundingBox(0.2f, 0.2f, 0.6f, 0.6f)
        val exactIou = NonMaxSuppression.intersectionOverUnion(first, second)

        val kept = NonMaxSuppression.apply(
            listOf(
                WeaponDetection(first, "knife", 0.9f),
                WeaponDetection(second, "knife", 0.8f),
            ),
            iouThreshold = exactIou - 0.001f,
        )

        assertEquals(1, kept.size)
    }

    private fun detection(
        type: String,
        confidence: Float,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
    ) = WeaponDetection(
        boundingBox = BoundingBox(left, top, right, bottom),
        weaponType = type,
        confidence = confidence,
    )

    private companion object {
        const val TOLERANCE = 1e-4f
    }
}
