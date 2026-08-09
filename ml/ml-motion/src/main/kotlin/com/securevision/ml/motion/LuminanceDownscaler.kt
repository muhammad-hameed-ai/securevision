package com.securevision.ml.motion

import android.graphics.Bitmap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reduces a frame to a small greyscale grid for comparison.
 *
 * Uses [Bitmap.createScaledBitmap] with filtering rather than sampling every
 * *n*th pixel: filtered downscaling averages each source region, so a person
 * moving across a few pixels still shifts the cell they occupy. Point sampling
 * would step over them entirely on some frames and register nothing.
 */
@Singleton
class LuminanceDownscaler @Inject constructor() {

    /**
     * Converts a frame to a [GRID_SIZE] square luminance grid.
     *
     * @param frame The frame to reduce.
     * @return The grid, ready for comparison.
     */
    fun downscale(frame: Bitmap): LuminanceGrid {
        val scaled = Bitmap.createScaledBitmap(frame, GRID_SIZE, GRID_SIZE, true)

        val pixels = IntArray(GRID_SIZE * GRID_SIZE)
        scaled.getPixels(pixels, 0, GRID_SIZE, 0, 0, GRID_SIZE, GRID_SIZE)

        // createScaledBitmap may return the original instance when no scaling was
        // needed; recycling that would destroy a frame still in use upstream.
        if (scaled !== frame) scaled.recycle()

        return LuminanceGrid(
            values = IntArray(pixels.size) { index -> pixels[index].luminance() },
            size = GRID_SIZE,
        )
    }

    /**
     * Rec. 601 luma from a packed ARGB pixel.
     *
     * Weighted rather than a plain channel average because the eye — and the
     * camera's own exposure control — respond mostly to green. An unweighted mean
     * makes a red-to-blue change of equal perceived brightness look like motion.
     */
    private fun Int.luminance(): Int {
        val red = (this shr 16) and 0xFF
        val green = (this shr 8) and 0xFF
        val blue = this and 0xFF

        return (red * RED_WEIGHT + green * GREEN_WEIGHT + blue * BLUE_WEIGHT) shr WEIGHT_SHIFT
    }

    companion object {
        /** Edge length of the comparison grid. */
        const val GRID_SIZE = 64

        // Rec. 601 coefficients scaled to integers to avoid float maths per pixel.
        private const val RED_WEIGHT = 77
        private const val GREEN_WEIGHT = 150
        private const val BLUE_WEIGHT = 29
        private const val WEIGHT_SHIFT = 8
    }
}
