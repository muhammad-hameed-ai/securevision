package com.securevision.ml.face.match

import com.securevision.core.model.EnrolledProfile
import kotlin.math.sqrt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Matching, and specifically the margin rule.
 *
 * The threshold alone decides "is this anyone I know". The margin decides "am I
 * sure which one" — and it is the margin that stops a security app confidently
 * naming the wrong person when two enrolled faces score almost identically.
 */
class FaceMatcherTest {

    private val matcher = FaceMatcher()

    @Test
    fun `identical unit vectors score one`() {
        val vector = unit(1f, 0f, 0f, 0f)

        assertEquals(1f, matcher.cosineSimilarity(vector, vector), TOLERANCE)
    }

    @Test
    fun `orthogonal unit vectors score zero`() {
        assertEquals(
            0f,
            matcher.cosineSimilarity(unit(1f, 0f, 0f, 0f), unit(0f, 1f, 0f, 0f)),
            TOLERANCE,
        )
    }

    @Test
    fun `opposite unit vectors score minus one`() {
        assertEquals(
            -1f,
            matcher.cosineSimilarity(unit(1f, 0f, 0f, 0f), unit(-1f, 0f, 0f, 0f)),
            TOLERANCE,
        )
    }

    @Test
    fun `comparing different lengths is an error, not a low score`() {
        val failure = assertThrows(IllegalArgumentException::class.java) {
            matcher.cosineSimilarity(FloatArray(512), FloatArray(128))
        }

        assertTrue(failure.message!!.contains("different models"))
    }

    @Test
    fun `accepts a clear winner`() {
        val query = unit(1f, 0f, 0f, 0f)
        val profiles = listOf(
            profile("ayesha", unit(0.99f, 0.14f, 0f, 0f)),
            profile("bilal", unit(0f, 1f, 0f, 0f)),
        )

        val outcome = matcher.findBestMatch(query, profiles, threshold = 0.75f, margin = 0.05f)

        assertTrue(outcome is MatchOutcome.Match)
        assertEquals("ayesha", (outcome as MatchOutcome.Match).profile.id)
    }

    @Test
    fun `rejects a winner that does not clear the threshold`() {
        val query = unit(1f, 0f, 0f, 0f)
        val profiles = listOf(profile("ayesha", unit(0.6f, 0.8f, 0f, 0f)))

        val outcome = matcher.findBestMatch(query, profiles, threshold = 0.75f, margin = 0.05f)

        assertTrue(outcome is MatchOutcome.NoMatch)
        assertEquals(0.6f, (outcome as MatchOutcome.NoMatch).bestScore, TOLERANCE)
    }

    @Test
    fun `rejects an ambiguous winner even when it clears the threshold`() {
        // Two enrolled people the query resembles almost equally. The top score is
        // comfortably above threshold, but naming either one would be a guess.
        val query = unit(1f, 0f, 0f, 0f)
        val profiles = listOf(
            profile("twin-a", unit(0.98f, 0.199f, 0f, 0f)),
            profile("twin-b", unit(0.97f, 0.243f, 0f, 0f)),
        )

        val outcome = matcher.findBestMatch(query, profiles, threshold = 0.75f, margin = 0.05f)

        assertTrue("ambiguous match must be rejected", outcome is MatchOutcome.NoMatch)
        assertTrue((outcome as MatchOutcome.NoMatch).bestScore > 0.75f)
    }

    @Test
    fun `a single enrolled profile can still be matched`() {
        // There is no runner-up to lead, so the margin rule must not block the very
        // first enrolment from ever being recognised.
        val query = unit(1f, 0f, 0f, 0f)
        val profiles = listOf(profile("only", unit(0.99f, 0.14f, 0f, 0f)))

        val outcome = matcher.findBestMatch(query, profiles, threshold = 0.75f, margin = 0.05f)

        assertTrue(outcome is MatchOutcome.Match)
    }

    @Test
    fun `no enrolled profiles yields no match`() {
        val outcome = matcher.findBestMatch(
            unit(1f, 0f, 0f, 0f),
            profiles = emptyList(),
            threshold = 0.75f,
            margin = 0.05f,
        )

        assertTrue(outcome is MatchOutcome.NoMatch)
        assertEquals(0, (outcome as MatchOutcome.NoMatch).incomparableProfiles)
    }

    @Test
    fun `profiles from a different model are skipped and counted`() {
        val query = FloatArray(512) { if (it == 0) 1f else 0f }
        val profiles = listOf(
            profile("old-model-a", FloatArray(128) { if (it == 0) 1f else 0f }),
            profile("old-model-b", FloatArray(128) { if (it == 0) 1f else 0f }),
        )

        val outcome = matcher.findBestMatch(query, profiles, threshold = 0.75f, margin = 0.05f)

        // Reported rather than scored: comparing across embedding spaces produces
        // meaningless numbers, and "re-enrol" is the only useful response.
        assertTrue(outcome is MatchOutcome.NoMatch)
        assertEquals(2, (outcome as MatchOutcome.NoMatch).incomparableProfiles)
    }

    @Test
    fun `a mixed set matches only the comparable profiles`() {
        val query = FloatArray(4) { if (it == 0) 1f else 0f }
        val profiles = listOf(
            profile("stale", FloatArray(8)),
            profile("current", unit(0.99f, 0.14f, 0f, 0f)),
        )

        val outcome = matcher.findBestMatch(query, profiles, threshold = 0.75f, margin = 0.05f)

        assertTrue(outcome is MatchOutcome.Match)
        assertEquals("current", (outcome as MatchOutcome.Match).profile.id)
    }

    private fun unit(vararg values: Float): FloatArray {
        var sumOfSquares = 0f
        for (value in values) sumOfSquares += value * value
        val magnitude = sqrt(sumOfSquares)

        return FloatArray(values.size) { values[it] / magnitude }
    }

    private fun profile(id: String, embedding: FloatArray) = EnrolledProfile(
        id = id,
        name = id,
        age = 30,
        photoUri = "file:///$id.jpg",
        embedding = embedding,
        isWatchlisted = false,
        createdAt = 1_754_000_000_000L,
    )

    private companion object {
        const val TOLERANCE = 1e-4f
    }
}
