package com.securevision.ml.weapon

/**
 * Provisional metadata for the weapon detector, implemented in Phase 5.
 *
 * The threshold is set higher than the general object detector's on purpose: a
 * weapon detection raises a critical alarm, so a false positive costs far more
 * here than a missed low-confidence frame.
 */
internal object WeaponModelSpec {

    /** Asset name of the TFLite detector. */
    const val ASSET_NAME = "weapon_detector.tflite"

    /** Square input edge, in pixels. */
    const val INPUT_SIZE = 320

    /** Detections below this score are discarded. */
    const val CONFIDENCE_THRESHOLD = 0.60f

    /** Consecutive frames a weapon must persist for before the alarm sounds. */
    const val CONFIRMATION_FRAMES = 3
}
