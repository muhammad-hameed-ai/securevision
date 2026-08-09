package com.securevision.core.domain.engine

import com.securevision.core.model.MotionResult

/**
 * Frame-to-frame motion detection.
 *
 * Stateful by nature: it holds the previous frame and the debounce clock, which
 * is why it is a long-lived engine rather than a pure function. Callers must
 * [reset] it whenever the frame source changes — a camera flip otherwise reads as
 * one enormous motion event.
 */
interface MotionDetectionEngine {

    /**
     * Compares a frame against the previous one.
     *
     * @param frame The frame to analyse.
     * @param intensityThreshold Fraction of changed pixels that counts as motion,
     *   from settings.
     * @return The comparison. The first frame after a [reset] always reports no
     *   motion, because there is nothing to compare it against.
     */
    suspend fun detect(frame: FaceFrame, intensityThreshold: Float): MotionResult

    /** Discards the previous frame and the debounce state. */
    fun reset()
}
