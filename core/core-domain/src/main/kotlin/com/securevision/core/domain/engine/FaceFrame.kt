package com.securevision.core.domain.engine

import android.graphics.Bitmap

/**
 * One camera frame handed to the recognition engine.
 *
 * @property bitmap The frame, already upright. Rotation is resolved before this
 *   point so no stage downstream has to reason about sensor orientation.
 * @property isFrontCamera Whether the frame came from the front camera. Carried
 *   because the preview is mirrored for the front camera while the analysis
 *   image is not, and the overlay has to undo exactly one of those.
 * @property timestampMillis When the frame was captured, epoch milliseconds UTC.
 */
data class FaceFrame(
    val bitmap: Bitmap,
    val isFrontCamera: Boolean,
    val timestampMillis: Long,
)
