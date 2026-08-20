package com.securevision.ml.face.match

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A confirmed identity survives a brief dropout, and only a brief one.
 *
 * Covers the real cases people reported: a hand passing over the face, a laugh, a
 * blink. Without the hold, one unmatched frame flips a recognised person to
 * UNKNOWN and the name flickers constantly.
 *
 * The expiry is a security property, not a tuning preference — a face that stays
 * unmatched must become UNKNOWN, or someone stepping into frame where a known
 * person just stood would inherit their name.
 */
class StickyIdentityTest {

    private val voter = MultiFrameVoter()
    private val required = 3
    private val start = 1_700_000_000_000L

    @Test
    fun `a confirmed identity survives a fully failed window`() {
        confirm(id = 1, profile = "khan", at = start)

        // Enough non-matching frames to flush every remembered vote out of the
        // sliding window. Fewer than this and the ordinary vote still carries the
        // name, which would make this test pass without exercising the hold at all.
        val result = missFrames(id = 1, count = MultiFrameVoter.WINDOW_SIZE, from = start + 300)

        assertTrue("expected the name to hold", result is VoteResult.Known)
        assertEquals("khan", (result as VoteResult.Known).profileId)
    }

    @Test
    fun `the identity expires once the window passes`() {
        confirm(id = 1, profile = "khan", at = start)

        // Flush the vote window first. The last frame in which the ordinary vote
        // still carried counts as a fresh confirmation and restarts the hold —
        // correct behaviour, and the reason this has to be done in two steps
        // rather than by jumping the clock straight after confirming.
        val heldAt = start + 300
        missFrames(id = 1, count = MultiFrameVoter.WINDOW_SIZE, from = heldAt)

        val result = voter.record(
            trackingId = 1,
            matchedProfileId = null,
            requiredAgreements = required,
            nowMillis = heldAt + MultiFrameVoter.STICKY_WINDOW_MILLIS + 1,
        )

        // The person has genuinely gone, or genuinely is not who we thought.
        assertTrue("a lapsed hold must report UNKNOWN", result is VoteResult.Unknown)
    }

    @Test
    fun `a face that was never recognised does not become known`() {
        // No confirmation first: repeated non-matches must stay UNKNOWN. The hold
        // can only extend an identity, never invent one.
        var result: VoteResult = VoteResult.Undecided
        repeat(4) { result = voter.record(2, matchedProfileId = null, required, start) }

        assertTrue(result is VoteResult.Unknown)
    }

    @Test
    fun `a new tracking id cannot inherit a previous identity`() {
        confirm(id = 1, profile = "khan", at = start)

        // A different person, tracked separately, arriving immediately after.
        var result: VoteResult = VoteResult.Undecided
        repeat(4) { result = voter.record(2, matchedProfileId = null, required, start + 100) }

        assertTrue("id 2 must not inherit id 1's name", result is VoteResult.Unknown)
    }

    @Test
    fun `dropping out of frame clears the hold`() {
        confirm(id = 1, profile = "khan", at = start)

        // The tracked face leaves; the id is pruned.
        voter.retainOnly(emptySet())

        var result: VoteResult = VoteResult.Undecided
        repeat(4) { result = voter.record(1, matchedProfileId = null, required, start + 100) }

        // Id 1 may be reused by an entirely different person on the next frame.
        assertTrue(result is VoteResult.Unknown)
    }

    @Test
    fun `reset clears every hold`() {
        confirm(id = 1, profile = "khan", at = start)

        voter.reset()

        var result: VoteResult = VoteResult.Undecided
        repeat(4) { result = voter.record(1, matchedProfileId = null, required, start + 100) }

        assertTrue(result is VoteResult.Unknown)
    }

    /** Votes a face KNOWN by agreeing on one profile across the required frames. */
    private fun confirm(id: Int, profile: String, at: Long) {
        repeat(required) { voter.record(id, profile, required, at) }
    }

    /**
     * Records [count] consecutive non-matching frames, all stamped [from].
     *
     * Enough of them to clear the sliding window, so what comes back is the
     * sticky hold's verdict rather than a leftover vote.
     */
    private fun missFrames(id: Int, count: Int, from: Long): VoteResult {
        var result: VoteResult = VoteResult.Undecided
        repeat(count) { result = voter.record(id, matchedProfileId = null, required, from) }

        return result
    }
}
