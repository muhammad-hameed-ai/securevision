package com.securevision.ml.face.match

import com.securevision.core.model.EnrolledProfile
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Scores a query embedding against the enrolled profiles.
 *
 * Two conditions must both hold before an identity is accepted: the best score
 * clears the threshold, **and** it leads the runner-up by a margin. The margin is
 * the part that prevents confident misidentification — a query that resembles two
 * enrolled people almost equally has not identified either of them, however high
 * the top score is, and picking the winner in that situation is exactly how a
 * security app names the wrong person.
 */
@Singleton
class FaceMatcher @Inject constructor() {

    /**
     * Cosine similarity between two embeddings.
     *
     * A plain dot product, valid only because both vectors are L2-normalised when
     * they are produced. Two vectors of different lengths came from different
     * models and are not comparable at all, so that is an error rather than a low
     * score.
     *
     * @throws IllegalArgumentException if the vectors differ in length.
     */
    fun cosineSimilarity(first: FloatArray, second: FloatArray): Float {
        require(first.size == second.size) {
            "embeddings are ${first.size} and ${second.size} long — they came from different models"
        }

        var dotProduct = 0f
        for (index in first.indices) dotProduct += first[index] * second[index]

        return dotProduct
    }

    /**
     * Finds the best match for a query embedding.
     *
     * @param query L2-normalised embedding of the aligned face.
     * @param profiles Enrolled profiles to score against.
     * @param threshold Minimum similarity for a match.
     * @param margin Minimum lead the best must hold over the second-best.
     * @return The outcome, carrying the score whether or not it was accepted so
     *   the UI can show how close a rejection was.
     */
    fun findBestMatch(
        query: FloatArray,
        profiles: List<EnrolledProfile>,
        threshold: Float,
        margin: Float,
    ): MatchOutcome {
        val comparable = profiles.filter { it.embeddingSize == query.size }

        if (comparable.isEmpty()) {
            return MatchOutcome.NoMatch(
                bestScore = 0f,
                // Distinguishes "nobody is enrolled" from "everyone enrolled used a
                // different model", which need entirely different fixes.
                incomparableProfiles = profiles.size,
            )
        }

        val scored = comparable
            .map { profile -> profile to cosineSimilarity(query, profile.embedding) }
            .sortedByDescending { (_, score) -> score }

        val (bestProfile, bestScore) = scored.first()
        val secondScore = scored.getOrNull(1)?.second ?: NO_RUNNER_UP

        val clearsThreshold = bestScore > threshold
        val clearsMargin = (bestScore - secondScore) > margin

        return if (clearsThreshold && clearsMargin) {
            MatchOutcome.Match(profile = bestProfile, score = bestScore, runnerUpScore = secondScore)
        } else {
            MatchOutcome.NoMatch(bestScore = bestScore, incomparableProfiles = 0)
        }
    }

    private companion object {
        /**
         * Stands in for the second-best score when only one profile is enrolled.
         *
         * Negative so the margin test always passes with a single profile —
         * otherwise the very first enrolment could never be recognised.
         */
        const val NO_RUNNER_UP = -1f
    }
}

/** The result of scoring one query against the enrolled set. */
sealed interface MatchOutcome {

    /**
     * An identity was accepted.
     *
     * @property profile The matched profile.
     * @property score Winning cosine similarity.
     * @property runnerUpScore Second-best score, for diagnostics.
     */
    data class Match(
        val profile: EnrolledProfile,
        val score: Float,
        val runnerUpScore: Float,
    ) : MatchOutcome

    /**
     * No identity was accepted.
     *
     * @property bestScore Highest score seen, even though it was rejected.
     * @property incomparableProfiles How many profiles were skipped because their
     *   embedding length did not match the active model's.
     */
    data class NoMatch(
        val bestScore: Float,
        val incomparableProfiles: Int,
    ) : MatchOutcome
}
