package com.securevision.core.model

/**
 * An axis-aligned rectangle in **normalised image coordinates**, where `0f` is the
 * left/top edge of the analysed frame and `1f` is the right/bottom edge.
 *
 * Normalised rather than pixel coordinates so that a box produced by the analysis
 * frame (typically 640×480) can be drawn onto a preview surface of any size and
 * orientation without the detector needing to know about the display.
 *
 * @property left Left edge, normally in `0f..1f`.
 * @property top Top edge, normally in `0f..1f`.
 * @property right Right edge, normally in `0f..1f`.
 * @property bottom Bottom edge, normally in `0f..1f`.
 */
data class BoundingBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {

    /** Horizontal extent of the box. */
    val width: Float get() = right - left

    /** Vertical extent of the box. */
    val height: Float get() = bottom - top

    /** Horizontal midpoint, useful for anchoring labels and tracking motion. */
    val centerX: Float get() = left + width / 2f

    /** Vertical midpoint, useful for anchoring labels and tracking motion. */
    val centerY: Float get() = top + height / 2f

    /** Area covered by the box; `0f` for a degenerate (inverted or empty) box. */
    val area: Float get() = if (width <= 0f || height <= 0f) 0f else width * height
}
