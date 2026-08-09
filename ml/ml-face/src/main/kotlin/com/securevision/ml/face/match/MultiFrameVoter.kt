package com.securevision.ml.face.match

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Requires several consecutive frames to agree before committing to an identity.
 *
 * A single frame is a weak signal: a blink, a motion blur or a moment of bad
 * light can drop a genuine match below threshold, and an unlucky crop can push a
 * stranger above it. Voting turns that into a majority decision — a known person
 * keeps their green box through a blurred frame, and a stranger cannot be named
 * by one lucky one.
 *
 * History is kept per tracking id, so two people in frame vote independently. Ids
 * that stop appearing are pruned rather than accumulating for the life of the
 * session.
 */
@Singleton
class MultiFrameVoter @Inject constructor() {

    private val histories = mutableMapOf<Int, ArrayDeque<String?>>()

    /**
     * Records one frame's verdict and returns the committed one.
     *
     * @param trackingId The tracked face this verdict belongs to.
     * @param matchedProfileId The profile matched this frame, or `null` for no match.
     * @param requiredAgreements How many of the last [WINDOW_SIZE] frames must
     *   agree. Comes from settings so it is tunable without a rebuild.
     * @return The committed verdict.
     */
    fun record(
        trackingId: Int,
        matchedProfileId: String?,
        requiredAgreements: Int,
    ): VoteResult {
        val history = histories.getOrPut(trackingId) { ArrayDeque(WINDOW_SIZE) }

        history.addLast(matchedProfileId)
        while (history.size > WINDOW_SIZE) history.removeFirst()

        // Not enough evidence yet. Reporting PROCESSING rather than guessing is
        // what stops a box flickering between a name and UNKNOWN on first sight.
        if (history.size < requiredAgreements) return VoteResult.Undecided

        val winner = history
            .filterNotNull()
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { (_, count) -> count }

        if (winner != null && winner.value >= requiredAgreements) {
            return VoteResult.Known(profileId = winner.key, agreeingFrames = winner.value)
        }

        val unmatchedFrames = history.count { it == null }
        if (unmatchedFrames >= requiredAgreements) {
            return VoteResult.Unknown(agreeingFrames = unmatchedFrames)
        }

        // Frames disagree with no clear majority either way — genuinely undecided,
        // which is different from "decided that this is a stranger".
        return VoteResult.Undecided
    }

    /**
     * Drops history for tracking ids no longer in frame.
     *
     * @param activeTrackingIds Ids seen in the current frame.
     */
    fun retainOnly(activeTrackingIds: Set<Int>) {
        histories.keys.retainAll(activeTrackingIds)
    }

    /** Clears all history, e.g. when the camera flips and tracking ids restart. */
    fun reset() {
        histories.clear()
    }

    companion object {
        /** How many recent frames are considered. */
        const val WINDOW_SIZE: Int = 4
    }
}

/** The committed verdict for a tracked face. */
sealed interface VoteResult {

    /**
     * Enough frames agreed on one identity.
     *
     * @property profileId The agreed profile.
     * @property agreeingFrames How many of the window agreed.
     */
    data class Known(val profileId: String, val agreeingFrames: Int) : VoteResult

    /**
     * Enough frames agreed that this face matches nobody.
     *
     * @property agreeingFrames How many of the window agreed.
     */
    data class Unknown(val agreeingFrames: Int) : VoteResult

    /** Too few frames, or no majority. The overlay shows this as still resolving. */
    data object Undecided : VoteResult
}
