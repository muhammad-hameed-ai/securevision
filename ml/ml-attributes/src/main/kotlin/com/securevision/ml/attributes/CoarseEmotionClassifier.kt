package com.securevision.ml.attributes

import javax.inject.Inject
import javax.inject.Singleton

/**
 * A coarse emotion signal derived from the detector's smile probability.
 *
 * Not a real emotion classifier and deliberately labelled as coarse: it can
 * separate "smiling" from "not smiling" and nothing else. It exists because ML
 * Kit already computes the smile score during detection, so this costs no extra
 * inference — and one honestly-labelled attribute is more useful than five that
 * are all `null`.
 *
 * Replaced wholesale when a real emotion model lands.
 */
@Singleton
class CoarseEmotionClassifier @Inject constructor() {

    /**
     * Maps a smile probability onto a label.
     *
     * @param smilingProbability Detector score in `0f..1f`, or `null` when
     *   classification was off or the detector declined.
     * @return A label, or `null` when nothing was assessed or the score sits in
     *   the ambiguous middle. Returning `null` there is the point: guessing
     *   "neutral" for a face the detector was unsure about would be a claim the
     *   evidence does not support.
     */
    fun classify(smilingProbability: Float?): String? {
        val probability = smilingProbability ?: return null

        return when {
            probability >= SMILING_THRESHOLD -> SMILING
            probability <= NEUTRAL_THRESHOLD -> NEUTRAL
            else -> null
        }
    }

    companion object {
        /** Above this the face is confidently smiling. */
        const val SMILING_THRESHOLD = 0.65f

        /** Below this the face is confidently not smiling. */
        const val NEUTRAL_THRESHOLD = 0.25f

        /** Label for a confidently smiling face. */
        const val SMILING = "smiling"

        /** Label for a confidently unsmiling face. */
        const val NEUTRAL = "neutral"
    }
}
