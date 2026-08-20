package com.securevision.ml.face.match

import com.securevision.core.model.BoundingBox
import com.securevision.core.model.FaceDetection
import com.securevision.core.model.FaceLandmarks
import com.securevision.core.model.NormalisedPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A region that is not a usable face must be rejected whatever the profile list
 * holds.
 *
 * The field report: with nobody enrolled, hands and fragments of objects drew
 * solid red UNKNOWN boxes — six of them in a scene containing one face. The cause
 * was the empty-profile guard returning UNKNOWN *before* the quality gate ran, so
 * every raw ML Kit candidate became a confirmed stranger. "Not a face" and
 * "a stranger" are different answers, and only one of them belongs on screen.
 */
class GateBeforeProfileGuardTest {

    private val gate = FaceQualityGate()

    /** 480 × 640, the portrait analysis frame. */
    private val portrait = 480f / 640f

    @Test
    fun `a hand-sized region is rejected`() {
        // Roughly 8% of the short edge — the size of the spurious candidates that
        // were drawing boxes.
        assertTrue(gate.assess(detection(relativeWidth = 0.08f), portrait) is FaceQuality.Rejected)
    }

    @Test
    fun `a face at arm's length is still accepted`() {
        // The floor has to reject clutter without rejecting the user. About 20% of
        // the short edge is a face held at a comfortable distance.
        assertEquals(
            FaceQuality.Acceptable,
            gate.assess(detection(relativeWidth = 0.20f), portrait),
        )
    }

    @Test
    fun `the raised floor sits between the two`() {
        // Pins the intent rather than the number: whatever the constant is, it
        // must reject clutter and accept a real face.
        assertTrue(FaceQualityGate.MIN_RELATIVE_WIDTH > 0.08f)
        assertTrue(FaceQualityGate.MIN_RELATIVE_WIDTH < 0.20f)
    }

    @Test
    fun `a region without landmarks is rejected before anything else`() {
        val noLandmarks = detection(relativeWidth = 0.40f).copy(landmarks = null)

        // Large enough to pass every other check. A hand fills the frame nicely
        // and has no eyes, nose or mouth — which is exactly how it is told apart
        // from a face.
        assertEquals(
            FaceQuality.Rejected(FaceQuality.Reason.NO_LANDMARKS),
            gate.assess(noLandmarks, portrait),
        )
    }

    private fun detection(relativeWidth: Float) = FaceDetection(
        trackingId = 1,
        boundingBox = BoundingBox(0.2f, 0.2f, 0.2f + relativeWidth, 0.6f),
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
