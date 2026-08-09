package com.securevision.ml.weapon.detect

import com.securevision.core.model.BoundingBox
import kotlin.math.min

/**
 * Fits a frame into the model's square input without distorting it.
 *
 * Object detectors are trained on undistorted images. Stretching a 4:3 frame into
 * a square would make every object wider than the detector expects, which
 * degrades scores across the board — subtly enough to look like a weak model
 * rather than a preprocessing bug.
 *
 * So the frame is scaled by the **smaller** ratio, leaving bars on two sides. Note
 * this is the opposite of the overlay's FILL_CENTER, which uses the larger ratio
 * and crops: there, hiding edges is acceptable; here, cropping could remove the
 * weapon.
 *
 * @property sourceWidth Frame width in pixels.
 * @property sourceHeight Frame height in pixels.
 * @property targetSize Model input edge in pixels.
 */
data class Letterbox(
    val sourceWidth: Int,
    val sourceHeight: Int,
    val targetSize: Int,
) {
    /** Uniform scale applied to the frame. */
    val scale: Float
        get() = if (sourceWidth <= 0 || sourceHeight <= 0) {
            1f
        } else {
            min(targetSize.toFloat() / sourceWidth, targetSize.toFloat() / sourceHeight)
        }

    /** Width of the scaled frame inside the square input. */
    val scaledWidth: Float get() = sourceWidth * scale

    /** Height of the scaled frame inside the square input. */
    val scaledHeight: Float get() = sourceHeight * scale

    /** Horizontal padding on each side. */
    val padX: Float get() = (targetSize - scaledWidth) / 2f

    /** Vertical padding on each side. */
    val padY: Float get() = (targetSize - scaledHeight) / 2f

    /**
     * Converts a detection from the model's padded space back to the frame.
     *
     * The step everyone forgets. A box that is correct in the letterboxed image is
     * offset by the padding and scaled wrong in the original — and, exactly like
     * the overlay transform, the error is smallest at the centre, so it reads as
     * imprecision rather than a bug.
     *
     * @param modelBox Detection in `0f..1f` of the **model input**, not the frame.
     * @return The same detection in `0f..1f` of the original frame, clamped to it.
     */
    fun toFrameSpace(modelBox: BoundingBox): BoundingBox {
        if (scaledWidth <= 0f || scaledHeight <= 0f) return modelBox

        fun mapX(value: Float) = ((value * targetSize) - padX) / scaledWidth
        fun mapY(value: Float) = ((value * targetSize) - padY) / scaledHeight

        return BoundingBox(
            left = mapX(modelBox.left).coerceIn(0f, 1f),
            top = mapY(modelBox.top).coerceIn(0f, 1f),
            right = mapX(modelBox.right).coerceIn(0f, 1f),
            bottom = mapY(modelBox.bottom).coerceIn(0f, 1f),
        )
    }
}
