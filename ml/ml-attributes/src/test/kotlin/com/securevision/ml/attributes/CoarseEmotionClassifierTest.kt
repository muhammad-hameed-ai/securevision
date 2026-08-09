package com.securevision.ml.attributes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The coarse emotion signal, and specifically when it declines to answer.
 *
 * Declining matters more than the labels: a face the detector was unsure about
 * must produce `null`, not a guessed "neutral" that an alert would then report as
 * fact.
 */
class CoarseEmotionClassifierTest {

    private val classifier = CoarseEmotionClassifier()

    @Test
    fun `an absent probability is not assessed`() {
        // Classification was off, or the detector declined. Nothing looked.
        assertNull(classifier.classify(null))
    }

    @Test
    fun `a confident smile is labelled smiling`() {
        assertEquals(CoarseEmotionClassifier.SMILING, classifier.classify(0.9f))
    }

    @Test
    fun `a confident non-smile is labelled neutral`() {
        assertEquals(CoarseEmotionClassifier.NEUTRAL, classifier.classify(0.05f))
    }

    @Test
    fun `the smiling threshold is inclusive`() {
        assertEquals(
            CoarseEmotionClassifier.SMILING,
            classifier.classify(CoarseEmotionClassifier.SMILING_THRESHOLD),
        )
    }

    @Test
    fun `the neutral threshold is inclusive`() {
        assertEquals(
            CoarseEmotionClassifier.NEUTRAL,
            classifier.classify(CoarseEmotionClassifier.NEUTRAL_THRESHOLD),
        )
    }

    @Test
    fun `the ambiguous middle is not assessed rather than guessed`() {
        assertNull(classifier.classify(0.45f))
        assertNull(classifier.classify(CoarseEmotionClassifier.SMILING_THRESHOLD - 0.01f))
        assertNull(classifier.classify(CoarseEmotionClassifier.NEUTRAL_THRESHOLD + 0.01f))
    }
}
