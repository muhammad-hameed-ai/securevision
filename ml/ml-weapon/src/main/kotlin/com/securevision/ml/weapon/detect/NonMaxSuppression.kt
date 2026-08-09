package com.securevision.ml.weapon.detect

import com.securevision.core.model.BoundingBox
import com.securevision.core.model.WeaponDetection
import kotlin.math.max
import kotlin.math.min

/**
 * Collapses overlapping detections of the same object into one.
 *
 * Object detectors emit many boxes per object — one per anchor that fired. Without
 * suppression a single knife draws six overlapping orange boxes and, worse, counts
 * as six weapons in the session stats and six alerts.
 *
 * Suppression is per class: a knife overlapping a person is two detections, not a
 * duplicate. Since this detector filters to weapon classes only, that mostly
 * matters when two different weapon types overlap.
 */
object NonMaxSuppression {

    /**
     * Keeps the highest-scoring box in each cluster of overlaps.
     *
     * @param detections Raw detections, in any order.
     * @param iouThreshold Overlap above which two boxes of the same class are
     *   treated as the same object.
     * @return Survivors, highest score first.
     */
    fun apply(
        detections: List<WeaponDetection>,
        iouThreshold: Float = DEFAULT_IOU_THRESHOLD,
    ): List<WeaponDetection> {
        if (detections.size <= 1) return detections

        val remaining = detections.sortedByDescending { it.confidence }.toMutableList()
        val kept = mutableListOf<WeaponDetection>()

        while (remaining.isNotEmpty()) {
            val best = remaining.removeAt(0)
            kept += best

            remaining.removeAll { candidate ->
                candidate.weaponType == best.weaponType &&
                    // Strictly greater: a candidate exactly at the threshold is
                    // kept. The boundary has to fall one way, and keeping an
                    // ambiguous box is the safer error for a weapon detector.
                    intersectionOverUnion(best.boundingBox, candidate.boundingBox) > iouThreshold
            }
        }

        return kept
    }

    /**
     * Overlap of two boxes as intersection area over union area.
     *
     * @return `0f` when they do not overlap, `1f` when identical.
     */
    fun intersectionOverUnion(first: BoundingBox, second: BoundingBox): Float {
        val intersectionLeft = max(first.left, second.left)
        val intersectionTop = max(first.top, second.top)
        val intersectionRight = min(first.right, second.right)
        val intersectionBottom = min(first.bottom, second.bottom)

        val intersectionWidth = intersectionRight - intersectionLeft
        val intersectionHeight = intersectionBottom - intersectionTop

        if (intersectionWidth <= 0f || intersectionHeight <= 0f) return 0f

        val intersection = intersectionWidth * intersectionHeight
        val union = first.area + second.area - intersection

        return if (union <= 0f) 0f else intersection / union
    }

    /** Overlap above which two same-class boxes are treated as one object. */
    const val DEFAULT_IOU_THRESHOLD = 0.45f
}
