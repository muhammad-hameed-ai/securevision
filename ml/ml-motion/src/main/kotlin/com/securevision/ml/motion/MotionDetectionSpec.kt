package com.securevision.ml.motion

/**
 * Provisional tuning for the frame-differencing motion detector, implemented in
 * Phase 5.
 *
 * Deliberately not a neural network: comparing downscaled luminance planes costs
 * a fraction of an inference pass and leaves the GPU free for face and weapon
 * detection, which is what keeps the live preview smooth.
 */
internal object MotionDetectionSpec {

    /** Edge length, in pixels, that frames are downscaled to before comparison. */
    const val COMPARISON_SIZE = 64

    /** Per-pixel luminance delta, 0..255, above which a pixel counts as changed. */
    const val PIXEL_DELTA_THRESHOLD = 25

    /** Fraction of changed pixels that constitutes motion. */
    const val CHANGED_PIXEL_RATIO_THRESHOLD = 0.02f

    /** Minimum gap between motion alerts, so sustained movement alerts once. */
    const val ALERT_COOLDOWN_MILLIS = 10_000L
}
