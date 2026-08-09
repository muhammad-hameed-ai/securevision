package com.securevision.core.domain.engine

import android.graphics.Bitmap
import com.securevision.core.model.AttributeAvailability
import com.securevision.core.model.FaceAttributes

/**
 * Soft attribute inference over an already-aligned face crop.
 *
 * Takes the crop the recognition pipeline has **already produced** rather than
 * re-detecting. Re-detecting would double the detector cost per frame and, worse,
 * could align slightly differently — so the attributes would describe a subtly
 * different crop from the one that was recognised.
 */
interface AttributeAnalysisEngine {

    /** Which classifiers are loaded. Absent ones yield `null`, never `false`. */
    val availability: AttributeAvailability

    /**
     * Loads whichever classifiers are present.
     *
     * @return What ended up available.
     */
    suspend fun prepare(): AttributeAvailability

    /**
     * Analyses an aligned crop.
     *
     * @param alignedFace The crop produced by the recognition pipeline's alignment
     *   stage — not a raw frame, and not a re-detected face.
     * @param smilingProbability ML Kit's smile score for this face when the
     *   detector supplied one, used for the coarse emotion signal. `null` when
     *   classification was off or the detector declined.
     * @return Whatever could be assessed. Every unavailable attribute is `null`.
     */
    suspend fun analyse(
        alignedFace: Bitmap,
        smilingProbability: Float? = null,
    ): FaceAttributes
}
