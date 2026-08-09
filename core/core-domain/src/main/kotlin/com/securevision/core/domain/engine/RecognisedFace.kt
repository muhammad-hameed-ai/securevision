package com.securevision.core.domain.engine

import android.graphics.Bitmap
import com.securevision.core.model.DetectionResult

/**
 * One recognised face, plus the aligned crop that produced it.
 *
 * The crop is carried out of the pipeline so attribute analysis can run on the
 * **same** image the embedder saw. Re-detecting instead would cost a second
 * detector pass and could align marginally differently, leaving the attributes
 * describing a slightly different crop from the one that was recognised.
 *
 * @property detection The recognition verdict for this face.
 * @property alignedCrop The 160×160 aligned crop, or `null` when the caller did
 *   not ask for crops or the face never reached the alignment stage. At roughly
 *   100 KB each these are cheap, but they are only produced on request so the
 *   common path — recognition with attributes switched off — allocates nothing.
 * @property smilingProbability ML Kit's smile score, when classification was on.
 */
class RecognisedFace(
    val detection: DetectionResult,
    val alignedCrop: Bitmap? = null,
    val smilingProbability: Float? = null,
) {
    /** Whether this face can be handed to attribute analysis. */
    val isAnalysable: Boolean get() = alignedCrop != null
}
