package com.securevision.feature.live.overlay

import com.securevision.core.model.BoundingBox
import kotlin.math.max

/**
 * Maps a detection's normalised box onto the preview surface.
 *
 * Four things compose here, and getting any one backwards puts every box in the
 * wrong place:
 *
 * 1. **Rotation** is already resolved — the analysis frame is upright before
 *    detection runs, so [analysisWidth] and [analysisHeight] describe the upright
 *    image, not the sensor's.
 * 2. **Normalisation** is already done — [BoundingBox] is in `0f..1f`, so nothing
 *    here depends on the analysis resolution.
 * 3. **Scale and crop** must use `max` of the two ratios, because `PreviewView`
 *    fills the viewport and centre-crops the overflow. Using `min` — the
 *    letterbox formula — is the classic error, and it fails in a way that is easy
 *    to miss: boxes sit correctly at the centre of the screen and drift further
 *    off the closer the subject moves to an edge.
 * 4. **Mirroring** applies to the front camera only. The preview is mirrored so
 *    the operator sees themselves as in a mirror, but the analysis image is not,
 *    so exactly one of the two has to be flipped back.
 *
 * Deliberately free of Android types so all four steps can be unit-tested rather
 * than verified by squinting at a phone.
 *
 * @property analysisWidth Width of the upright analysis frame, in pixels.
 * @property analysisHeight Height of the upright analysis frame, in pixels.
 * @property viewWidth Width of the preview surface, in pixels.
 * @property viewHeight Height of the preview surface, in pixels.
 * @property isFrontCamera Whether the preview is mirrored.
 */
data class OverlayTransform(
    val analysisWidth: Int,
    val analysisHeight: Int,
    val viewWidth: Float,
    val viewHeight: Float,
    val isFrontCamera: Boolean,
) {

    /**
     * Scale factor `PreviewView` applies in FILL_CENTER.
     *
     * `max`, not `min`: the image is scaled until it covers the viewport in both
     * axes, and the excess is cropped.
     */
    val scale: Float
        get() = if (analysisWidth <= 0 || analysisHeight <= 0) {
            1f
        } else {
            max(viewWidth / analysisWidth, viewHeight / analysisHeight)
        }

    /** Horizontal offset of the scaled image relative to the viewport. */
    val offsetX: Float get() = (viewWidth - analysisWidth * scale) / 2f

    /** Vertical offset of the scaled image relative to the viewport. */
    val offsetY: Float get() = (viewHeight - analysisHeight * scale) / 2f

    /**
     * Projects a normalised detection box into view coordinates.
     *
     * @param box The detection's box, in `0f..1f` frame space.
     * @return The same box in preview pixels, mirrored when appropriate.
     */
    fun project(box: BoundingBox): ViewRect {
        val left = box.left * analysisWidth * scale + offsetX
        val right = box.right * analysisWidth * scale + offsetX
        val top = box.top * analysisHeight * scale + offsetY
        val bottom = box.bottom * analysisHeight * scale + offsetY

        return if (isFrontCamera) {
            // Reflect about the viewport's vertical centre line. Left and right
            // swap, so they are re-ordered to keep left < right.
            ViewRect(
                left = viewWidth - right,
                top = top,
                right = viewWidth - left,
                bottom = bottom,
            )
        } else {
            ViewRect(left = left, top = top, right = right, bottom = bottom)
        }
    }
}

/**
 * A rectangle in preview-surface pixels.
 *
 * @property left Left edge.
 * @property top Top edge.
 * @property right Right edge.
 * @property bottom Bottom edge.
 */
data class ViewRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    /** Width in pixels. */
    val width: Float get() = right - left

    /** Height in pixels. */
    val height: Float get() = bottom - top
}
