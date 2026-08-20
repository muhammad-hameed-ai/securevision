package com.securevision.feature.live.overlay

import com.securevision.core.model.BoundingBox
import com.securevision.core.model.DetectionResult
import com.securevision.core.model.MatchStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Green must mean recognised, and only that.
 *
 * The field bug this pins: in landscape every face fell into the unresolved path,
 * which was painted `#00C9A7` — indistinguishable from the `#00D97E` used for a
 * genuine match — and labelled "…". The result was a green box with no name for
 * enrolled and unenrolled people alike. A false positive in a security product.
 *
 * These tests assert the rule that makes it unrepresentable: a face is drawn as
 * KNOWN only when there is an identity to show.
 */
class OverlayVerdictColourTest {

    @Test
    fun `a known face with an identity draws as known`() {
        val detection = detection(
            status = MatchStatus.KNOWN,
            profileId = "ayesha-1",
            profileName = "Ayesha",
        )

        assertEquals(MatchStatus.KNOWN, detection.displayStatus())
        assertTrue(detection.hasResolvedIdentity())
    }

    @Test
    fun `a known verdict without a profile id is downgraded`() {
        // Green claims "I recognise this person". Without an id there is nobody
        // to have recognised, so the claim must not be made.
        val detection = detection(
            status = MatchStatus.KNOWN,
            profileId = null,
            profileName = "Ayesha",
        )

        assertEquals(MatchStatus.PROCESSING, detection.displayStatus())
        assertFalse(detection.hasResolvedIdentity())
    }

    @Test
    fun `a known verdict without a name is downgraded`() {
        // A green box with no name was the second half of the reported bug.
        val detection = detection(
            status = MatchStatus.KNOWN,
            profileId = "ayesha-1",
            profileName = null,
        )

        assertEquals(MatchStatus.PROCESSING, detection.displayStatus())
    }

    @Test
    fun `a blank name is treated as no name`() {
        val detection = detection(
            status = MatchStatus.KNOWN,
            profileId = "ayesha-1",
            profileName = "   ",
        )

        assertEquals(MatchStatus.PROCESSING, detection.displayStatus())
    }

    @Test
    fun `an unknown face is never downgraded to processing`() {
        // UNKNOWN is a decision. Muddling it into "still thinking" would hide a
        // confirmed stranger.
        val detection = detection(status = MatchStatus.UNKNOWN, profileId = null, profileName = null)

        assertEquals(MatchStatus.UNKNOWN, detection.displayStatus())
    }

    @Test
    fun `an unresolved face stays unresolved`() {
        val detection =
            detection(status = MatchStatus.PROCESSING, profileId = null, profileName = null)

        assertEquals(MatchStatus.PROCESSING, detection.displayStatus())
    }

    @Test
    fun `no detection state can produce known without an identity`() {
        // Exhaustive: whatever the engine reports, green requires an identity.
        MatchStatus.entries.forEach { status ->
            val detection = detection(status = status, profileId = null, profileName = null)

            assertFalse(
                "status $status produced KNOWN with no identity",
                detection.displayStatus() == MatchStatus.KNOWN,
            )
        }
    }

    private fun detection(
        status: MatchStatus,
        profileId: String?,
        profileName: String?,
    ) = DetectionResult(
        trackingId = 1,
        boundingBox = BoundingBox(0.3f, 0.3f, 0.6f, 0.7f),
        matchStatus = status,
        profileId = profileId,
        profileName = profileName,
        confidence = 0.98f,
    )
}
