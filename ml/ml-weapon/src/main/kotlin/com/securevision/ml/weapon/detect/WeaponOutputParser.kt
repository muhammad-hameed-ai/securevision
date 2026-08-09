package com.securevision.ml.weapon.detect

import com.securevision.core.model.BoundingBox
import com.securevision.core.model.WeaponDetection

/**
 * Turns a raw YOLOv8 output tensor into detections.
 *
 * Kept separate from the interpreter so the parsing — the part most likely to be
 * subtly wrong — can be tested against synthetic tensors without a model file.
 *
 * Ultralytics exports emit `[1, 4 + classes, anchors]`: four box rows, then one
 * score row per class, each column an anchor. Some tool-chains transpose that to
 * `[1, anchors, 4 + classes]`. Both are handled, chosen by inspecting the shape,
 * because reading one as the other produces detections that are numerically valid
 * and completely wrong.
 */
object WeaponOutputParser {

    /**
     * Parses one output tensor.
     *
     * @param output Flattened model output.
     * @param shape The output tensor's shape, as reported by the interpreter.
     * @param confidenceThreshold Minimum class score to keep.
     * @param inputSize Model input edge, used to normalise pixel-space boxes.
     * @return Detections in model-input space, before letterbox correction and
     *   suppression. Empty when the shape is not one this parser understands.
     */
    fun parse(
        output: FloatArray,
        shape: IntArray,
        confidenceThreshold: Float,
        inputSize: Int,
    ): List<WeaponDetection> {
        val layout = Layout.from(shape, expectedChannels()) ?: return emptyList()

        val detections = mutableListOf<WeaponDetection>()

        for (anchor in 0 until layout.anchorCount) {
            var bestClass = -1
            var bestScore = 0f

            for (classIndex in 0 until layout.classCount) {
                val score = output[layout.indexOf(BOX_VALUES + classIndex, anchor)]
                if (score > bestScore) {
                    bestScore = score
                    bestClass = classIndex
                }
            }

            if (bestScore < confidenceThreshold) continue

            val label = WeaponClassMap.labelFor(bestClass) ?: continue

            val centreX = output[layout.indexOf(0, anchor)]
            val centreY = output[layout.indexOf(1, anchor)]
            val width = output[layout.indexOf(2, anchor)]
            val height = output[layout.indexOf(3, anchor)]

            detections += WeaponDetection(
                boundingBox = toBoundingBox(centreX, centreY, width, height, inputSize),
                weaponType = label,
                confidence = bestScore,
            )
        }

        return detections
    }

    /**
     * Converts centre-form to corner-form, normalising if the model emits pixels.
     *
     * Ultralytics exports vary: some normalise to `0..1`, some leave coordinates
     * in input pixels. Rather than guess from the export flags, the values decide
     * — anything meaningfully above 1 cannot be a normalised coordinate.
     */
    private fun toBoundingBox(
        centreX: Float,
        centreY: Float,
        width: Float,
        height: Float,
        inputSize: Int,
    ): BoundingBox {
        val divisor = if (maxOf(centreX, centreY, width, height) > NORMALISED_LIMIT) {
            inputSize.toFloat()
        } else {
            1f
        }

        val cx = centreX / divisor
        val cy = centreY / divisor
        val w = width / divisor
        val h = height / divisor

        return BoundingBox(
            left = (cx - w / 2f).coerceIn(0f, 1f),
            top = (cy - h / 2f).coerceIn(0f, 1f),
            right = (cx + w / 2f).coerceIn(0f, 1f),
            bottom = (cy + h / 2f).coerceIn(0f, 1f),
        )
    }

    /**
     * Which of the two known output layouts a tensor uses.
     *
     * @property anchorCount Number of candidate boxes.
     * @property classCount Number of class score rows.
     * @property channelsMajor `true` for `[1, 4+classes, anchors]`.
     */
    internal data class Layout(
        val anchorCount: Int,
        val classCount: Int,
        val channelsMajor: Boolean,
    ) {
        /** Index into the flattened tensor for one channel of one anchor. */
        fun indexOf(channel: Int, anchor: Int): Int =
            if (channelsMajor) channel * anchorCount + anchor
            else anchor * (BOX_VALUES + classCount) + channel

        companion object {
            /**
             * Infers the layout by matching a dimension against the expected
             * channel count.
             *
             * Deliberately not a size heuristic. "Anchors outnumber channels" is
             * true of real exports but is still a guess, and it fails outright on
             * small tensors. Since a model whose class count disagrees with
             * [WeaponClassMap] is rejected anyway, the expected channel count is
             * already known — so use it to identify the axis rather than infer it.
             *
             * @param shape The output tensor's shape.
             * @param expectedChannels `4 + WeaponClassMap.EXPECTED_CLASS_COUNT`.
             * @return The layout, or `null` when neither axis matches — which
             *   means the model does not fit this class map and must be refused
             *   rather than misread.
             */
            fun from(shape: IntArray, expectedChannels: Int): Layout? {
                if (shape.size != EXPECTED_RANK) return null

                val first = shape[1]
                val second = shape[2]

                return when {
                    // Ultralytics' own layout, so it wins the ambiguous square case.
                    first == expectedChannels -> Layout(
                        anchorCount = second,
                        classCount = first - BOX_VALUES,
                        channelsMajor = true,
                    )
                    second == expectedChannels -> Layout(
                        anchorCount = first,
                        classCount = second - BOX_VALUES,
                        channelsMajor = false,
                    )
                    else -> null
                }?.takeIf { it.classCount > 0 && it.anchorCount > 0 }
            }
        }
    }

    /** Channels a conforming model must emit: four box values plus one score per class. */
    fun expectedChannels(): Int = BOX_VALUES + WeaponClassMap.EXPECTED_CLASS_COUNT

    /** cx, cy, w, h. */
    private const val BOX_VALUES = 4

    /** `[batch, a, b]`. */
    private const val EXPECTED_RANK = 3

    /** Above this a coordinate cannot be normalised, so it must be in pixels. */
    private const val NORMALISED_LIMIT = 1.5f
}
