package com.securevision.ml.face.match

import com.securevision.core.model.BoundingBox
import com.securevision.core.model.FaceDetection
import com.securevision.core.model.FaceLandmarks
import com.securevision.core.model.NormalisedPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The same physical face must pass the gate in both orientations.
 *
 * The field bug: the bounding box is normalised against frame *width*, so the
 * same face is a smaller fraction of a 640-wide landscape frame than of a
 * 480-wide portrait one. Faces accepted in portrait were silently rejected in
 * landscape as TOO_SMALL — and a rejected face rendered identically to an
 * unresolved one, which rendered green.
 *
 * Measuring against the shorter edge removes the orientation from the maths.
 */
class OrientationInvarianceTest {

    private val gate = FaceQualityGate()

    /** 480 × 640 — the analysis frame in portrait. */
    private val portraitAspect = 480f / 640f

    /** 640 × 480 — the same stream with the phone turned. */
    private val landscapeAspect = 640f / 480f

    @Test
    fun `a face accepted in portrait is accepted in landscape`() {
        // 100 px on a 480-wide portrait frame: 0.208 of the width.
        val portrait = detection(relativeWidth = 100f / 480f)
        // The same 100 px face on the 640-wide landscape frame: 0.156.
        val landscape = detection(relativeWidth = 100f / 640f)

        assertEquals(FaceQuality.Acceptable, gate.assess(portrait, portraitAspect))
        assertEquals(FaceQuality.Acceptable, gate.assess(landscape, landscapeAspect))
    }

    @Test
    fun `a marginal face behaves the same in both orientations`() {
        // 60 px: 0.125 of a portrait frame — comfortably over the 0.10 floor —
        // but only 0.094 of a landscape one, which the old gate rejected.
        val portrait = detection(relativeWidth = 60f / 480f)
        val landscape = detection(relativeWidth = 60f / 640f)

        assertEquals(
            gate.assess(portrait, portraitAspect),
            gate.assess(landscape, landscapeAspect),
        )
    }

    @Test
    fun `a genuinely tiny face is still rejected in both orientations`() {
        // The gate must not have been loosened into uselessness: 20 px is too
        // few pixels to embed, whichever way the phone is held.
        val portrait = detection(relativeWidth = 20f / 480f)
        val landscape = detection(relativeWidth = 20f / 640f)

        assertTrue(gate.assess(portrait, portraitAspect) is FaceQuality.Rejected)
        assertTrue(gate.assess(landscape, landscapeAspect) is FaceQuality.Rejected)
    }

    @Test
    fun `the rejection reason is still reported as too small`() {
        val rejected = gate.assess(detection(relativeWidth = 0.01f), landscapeAspect)

        assertEquals(
            FaceQuality.Rejected(FaceQuality.Reason.TOO_SMALL),
            rejected,
        )
    }

    @Test
    fun `the gate reaches the same verdict in both orientations across the range`() {
        // Parity, swept rather than spot-checked: for a face of any given physical
        // size, portrait and landscape must agree. A threshold that disagreed
        // anywhere in this range is exactly what made landscape "not work".
        val physicalWidths = listOf(40f, 60f, 62f, 64f, 80f, 100f, 140f, 200f)

        physicalWidths.forEach { pixels ->
            val portraitVerdict = gate.assess(detection(pixels / 480f), portraitAspect)
            val landscapeVerdict = gate.assess(detection(pixels / 640f), landscapeAspect)

            assertEquals(
                "orientations disagreed for a ${pixels.toInt()}px face",
                portraitVerdict,
                landscapeVerdict,
            )
        }
    }

    @Test
    fun `a face tilted by phone rotation is not rejected`() {
        // Rotating the phone shifts a standing person's apparent roll. The old
        // 30-degree limit rejected them as TOO_TILTED, silently, and the screen
        // showed the same unresolved box it shows for everything else.
        val tilted = detection(100f / 640f).copy(rollDegrees = 40f)

        assertEquals(FaceQuality.Acceptable, gate.assess(tilted, landscapeAspect))
    }

    @Test
    fun `closed eyes and expression are not gate inputs at all`() {
        // Recorded as a fact rather than a behaviour change: the gate reads
        // landmarks, size, yaw and roll. Nothing looks at eye openness or
        // smiling, so neither can cause a rejection — a laugh or a blink that
        // loses a match did so at the matcher, not here.
        val gateInputs = listOf("landmarks", "boundingBox.width", "absoluteYaw", "absoluteRoll")

        assertTrue(gateInputs.none { it.contains("eye", ignoreCase = true) })
        assertTrue(gateInputs.none { it.contains("smil", ignoreCase = true) })
    }

    private fun detection(relativeWidth: Float) = FaceDetection(
        trackingId = 1,
        boundingBox = BoundingBox(
            left = 0.2f,
            top = 0.2f,
            right = 0.2f + relativeWidth,
            bottom = 0.6f,
        ),
        landmarks = FaceLandmarks(
            leftEye = NormalisedPoint(0.30f, 0.30f),
            rightEye = NormalisedPoint(0.40f, 0.30f),
            noseBase = NormalisedPoint(0.35f, 0.40f),
            leftMouth = NormalisedPoint(0.32f, 0.50f),
            rightMouth = NormalisedPoint(0.38f, 0.50f),
        ),
        yawDegrees = 0f,
        rollDegrees = 0f,
    )
}
