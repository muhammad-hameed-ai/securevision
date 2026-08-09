package com.securevision.ml.motion

/**
 * A downscaled greyscale representation of one frame.
 *
 * Motion is measured by comparing these rather than full frames. A 64×64 grid is
 * 4,096 comparisons instead of roughly 300,000, and the downscale doubles as a
 * blur — which is what stops sensor noise and JPEG-like compression artefacts
 * from reading as movement in a completely still room.
 *
 * @property values Luminance per cell, `0..255`, row-major.
 * @property size Edge length of the square grid.
 */
class LuminanceGrid(
    val values: IntArray,
    val size: Int,
) {
    init {
        require(values.size == size * size) {
            "grid is ${values.size} values but $size x $size needs ${size * size}"
        }
    }

    /**
     * Fraction of cells that differ from [other] by more than [perCellThreshold].
     *
     * @param other The previous frame's grid; must be the same size.
     * @param perCellThreshold Luminance delta, `0..255`, above which a cell counts
     *   as changed.
     * @return Changed-cell ratio in `0f..1f`.
     * @throws IllegalArgumentException if the grids differ in size, which would
     *   mean the frame source changed without a reset.
     */
    fun changedRatioAgainst(other: LuminanceGrid, perCellThreshold: Int): Float {
        require(size == other.size) {
            "grid sizes differ: $size vs ${other.size} — the frame source changed without a reset"
        }

        var changed = 0
        for (index in values.indices) {
            val delta = values[index] - other.values[index]
            if (delta > perCellThreshold || -delta > perCellThreshold) changed++
        }

        return changed.toFloat() / values.size
    }
}
