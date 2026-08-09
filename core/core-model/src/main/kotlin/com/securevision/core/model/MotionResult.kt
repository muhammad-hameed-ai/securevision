package com.securevision.core.model

/**
 * The outcome of comparing one frame against the previous one.
 *
 * Motion is derived arithmetically rather than by a neural network: comparing
 * downscaled luminance planes costs a fraction of an inference pass and leaves
 * the accelerator free for face and weapon detection, which is what keeps the
 * live preview smooth.
 *
 * @property hasMotion Whether the changed-pixel ratio crossed the threshold.
 * @property intensity Fraction of compared pixels that changed, in `0f..1f`. Kept
 *   even when [hasMotion] is false so the UI can show a live meter rather than a
 *   binary lamp, and so a threshold can be tuned against observed values.
 * @property isNewEvent Whether this crossing starts a new event, as opposed to
 *   continuing one already in progress. Only a new event should raise an alert —
 *   without this, someone walking across a room generates one alert per frame.
 */
data class MotionResult(
    val hasMotion: Boolean,
    val intensity: Float,
    val isNewEvent: Boolean,
) {
    companion object {
        /** Nothing moved, and nothing was even compared — the first frame of a session. */
        val NONE = MotionResult(hasMotion = false, intensity = 0f, isNewEvent = false)
    }
}
