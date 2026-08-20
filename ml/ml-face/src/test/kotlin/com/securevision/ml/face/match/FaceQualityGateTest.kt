package com.securevision.ml.face.match

import com.securevision.core.model.BoundingBox
import com.securevision.core.model.FaceDetection
import com.securevision.core.model.FaceLandmarks
import com.securevision.core.model.NormalisedPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Each threshold checked just inside and just outside its limit.
 *
 * The gate exists to keep faces that cannot be aligned well out of the embedder,
 * so a boundary that is off by a fraction admits exactly the crops that produce
 * unreliable matches.
 */
class FaceQualityGateTest {

    private val gate = FaceQualityGate()

    @Test
    fun `accepts a well presented face`() {
        assertEquals(FaceQuality.Acceptable, gate.assess(detection()))
    }

    @Test
    fun `rejects a face without landmarks`() {
        val result = gate.assess(detection(landmarks = null))

        assertReason(result, FaceQuality.Reason.NO_LANDMARKS)
    }

    @Test
    fun `rejects a face narrower than the minimum`() {
        val result = gate.assess(detection(width = 0.09f))

        assertReason(result, FaceQuality.Reason.TOO_SMALL)
    }

    @Test
    fun `accepts a face exactly at the minimum width`() {
        // Reads the constant rather than repeating it. The literal 0.10 here went
        // stale the moment the floor was raised to reject hands and object
        // fragments, and a boundary test that needs editing whenever the boundary
        // moves is testing the number, not the behaviour.
        //
        // Nudged clear of the boundary because the fixture derives the box as
        // `left + width`, and that round-trip lands a fraction under the floor —
        // an exactly-on-the-line assertion would be testing float representation
        // rather than the gate.
        assertEquals(
            FaceQuality.Acceptable,
            gate.assess(detection(width = FaceQualityGate.MIN_RELATIVE_WIDTH + EPSILON)),
        )
    }

    @Test
    fun `rejects a face just below the minimum width`() {
        assertReason(
            gate.assess(detection(width = FaceQualityGate.MIN_RELATIVE_WIDTH - EPSILON)),
            FaceQuality.Reason.TOO_SMALL,
        )
    }

    @Test
    fun `rejects a face turned beyond the yaw limit`() {
        assertReason(gate.assess(detection(yaw = 36f)), FaceQuality.Reason.TURNED_AWAY)
        assertReason(gate.assess(detection(yaw = -36f)), FaceQuality.Reason.TURNED_AWAY)
    }

    @Test
    fun `accepts a face exactly at the yaw limit`() {
        assertEquals(FaceQuality.Acceptable, gate.assess(detection(yaw = 35f)))
        assertEquals(FaceQuality.Acceptable, gate.assess(detection(yaw = -35f)))
    }

    @Test
    fun `rejects a face tilted beyond the roll limit`() {
        // Reads the constant. The literal 31 went stale when the limit was raised
        // to stop landscape faces being rejected as tilted.
        val beyond = FaceQualityGate.MAX_ROLL_DEGREES + 1f

        assertReason(gate.assess(detection(roll = beyond)), FaceQuality.Reason.TOO_TILTED)
        assertReason(gate.assess(detection(roll = -beyond)), FaceQuality.Reason.TOO_TILTED)
    }

    @Test
    fun `accepts a face exactly at the roll limit`() {
        assertEquals(
            FaceQuality.Acceptable,
            gate.assess(detection(roll = FaceQualityGate.MAX_ROLL_DEGREES)),
        )
    }

    @Test
    fun `tolerates the tilt introduced by rotating the phone`() {
        // A standing person's apparent roll shifts when the device turns. 40
        // degrees is a realistic landscape reading and must survive the gate.
        assertEquals(FaceQuality.Acceptable, gate.assess(detection(roll = 40f)))
    }

    @Test
    fun `missing landmarks are reported before any pose problem`() {
        // A face can fail several ways at once; the landmark check comes first
        // because it is the one that makes alignment impossible rather than merely
        // difficult.
        val result = gate.assess(detection(landmarks = null, width = 0.01f, yaw = 80f))

        assertReason(result, FaceQuality.Reason.NO_LANDMARKS)
    }

    private fun assertReason(quality: FaceQuality, expected: FaceQuality.Reason) {
        assertTrue("expected a rejection but was $quality", quality is FaceQuality.Rejected)
        assertEquals(expected, (quality as FaceQuality.Rejected).reason)
    }

    /** Clear of the float round-trip in the fixture's left + width arithmetic. */
    private val EPSILON = 0.005f

    private fun detection(
        width: Float = 0.30f,
        yaw: Float = 0f,
        roll: Float = 0f,
        landmarks: FaceLandmarks? = validLandmarks(),
    ) = FaceDetection(
        trackingId = 1,
        boundingBox = BoundingBox(left = 0.2f, top = 0.2f, right = 0.2f + width, bottom = 0.6f),
        landmarks = landmarks,
        yawDegrees = yaw,
        rollDegrees = roll,
    )

    private fun validLandmarks() = FaceLandmarks(
        leftEye = NormalisedPoint(0.30f, 0.32f),
        rightEye = NormalisedPoint(0.42f, 0.32f),
        noseBase = NormalisedPoint(0.36f, 0.40f),
        leftMouth = NormalisedPoint(0.31f, 0.48f),
        rightMouth = NormalisedPoint(0.41f, 0.48f),
    )
}
