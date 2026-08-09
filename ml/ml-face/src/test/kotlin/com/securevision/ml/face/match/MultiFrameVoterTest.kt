package com.securevision.ml.face.match

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The 3-of-4 majority rule that stops boxes flickering between a name and UNKNOWN. */
class MultiFrameVoterTest {

    private val voter = MultiFrameVoter()

    @Test
    fun `stays undecided until enough frames have been seen`() {
        assertEquals(VoteResult.Undecided, voter.record(TRACK, "ayesha", REQUIRED))
        assertEquals(VoteResult.Undecided, voter.record(TRACK, "ayesha", REQUIRED))
    }

    @Test
    fun `commits on three agreeing frames`() {
        repeat(2) { voter.record(TRACK, "ayesha", REQUIRED) }

        val result = voter.record(TRACK, "ayesha", REQUIRED)

        assertTrue(result is VoteResult.Known)
        assertEquals("ayesha", (result as VoteResult.Known).profileId)
        assertEquals(3, result.agreeingFrames)
    }

    @Test
    fun `two of four is not enough`() {
        voter.record(TRACK, "ayesha", REQUIRED)
        voter.record(TRACK, "bilal", REQUIRED)
        voter.record(TRACK, "ayesha", REQUIRED)
        val result = voter.record(TRACK, "bilal", REQUIRED)

        assertEquals(VoteResult.Undecided, result)
    }

    @Test
    fun `a single disagreeing frame does not break a committed identity`() {
        // The whole point: one blurred frame must not flip a recognised person.
        repeat(3) { voter.record(TRACK, "ayesha", REQUIRED) }

        val result = voter.record(TRACK, null, REQUIRED)

        assertTrue(result is VoteResult.Known)
        assertEquals(3, (result as VoteResult.Known).agreeingFrames)
    }

    @Test
    fun `commits unknown when enough frames match nobody`() {
        repeat(2) { voter.record(TRACK, null, REQUIRED) }

        val result = voter.record(TRACK, null, REQUIRED)

        assertTrue(result is VoteResult.Unknown)
        assertEquals(3, (result as VoteResult.Unknown).agreeingFrames)
    }

    @Test
    fun `the window slides so an old identity ages out`() {
        repeat(3) { voter.record(TRACK, "ayesha", REQUIRED) }
        // Four further frames push every "ayesha" out of the four-frame window.
        repeat(4) { voter.record(TRACK, "bilal", REQUIRED) }

        val result = voter.record(TRACK, "bilal", REQUIRED)

        assertEquals("bilal", (result as VoteResult.Known).profileId)
    }

    @Test
    fun `each tracking id votes independently`() {
        repeat(3) { voter.record(trackingId = 1, matchedProfileId = "ayesha", REQUIRED) }
        repeat(3) { voter.record(trackingId = 2, matchedProfileId = "bilal", REQUIRED) }

        val first = voter.record(trackingId = 1, matchedProfileId = "ayesha", REQUIRED)
        val second = voter.record(trackingId = 2, matchedProfileId = "bilal", REQUIRED)

        assertEquals("ayesha", (first as VoteResult.Known).profileId)
        assertEquals("bilal", (second as VoteResult.Known).profileId)
    }

    @Test
    fun `retainOnly drops history for faces that left the frame`() {
        repeat(3) { voter.record(trackingId = 1, matchedProfileId = "ayesha", REQUIRED) }

        voter.retainOnly(setOf(2))

        // Track 1 starts over, so it cannot commit on its next single frame.
        assertEquals(
            VoteResult.Undecided,
            voter.record(trackingId = 1, matchedProfileId = "ayesha", REQUIRED),
        )
    }

    @Test
    fun `reset clears every history`() {
        repeat(3) { voter.record(TRACK, "ayesha", REQUIRED) }

        voter.reset()

        assertEquals(VoteResult.Undecided, voter.record(TRACK, "ayesha", REQUIRED))
    }

    @Test
    fun `a stricter requirement needs more agreement`() {
        val strict = 4
        repeat(3) { voter.record(TRACK, "ayesha", strict) }

        assertEquals(VoteResult.Undecided, voter.record(TRACK, null, strict))
        assertTrue(voter.record(TRACK, "ayesha", strict) is VoteResult.Undecided)
    }

    private companion object {
        const val TRACK = 7
        const val REQUIRED = 3
    }
}
