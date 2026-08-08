package com.securevision.ml.objects

/**
 * Provisional metadata for the general object and person detector, implemented
 * in Phase 4. Values are confirmed against the shipped model at that point.
 *
 * The package is `objects` rather than `object` because `object` is a Kotlin
 * keyword; the Gradle module keeps the `ml-object` name.
 */
internal object ObjectModelSpec {

    /** Asset name of the TFLite detector. */
    const val ASSET_NAME = "object_detector.tflite"

    /** Square input edge, in pixels. */
    const val INPUT_SIZE = 320

    /** Detections below this score are discarded before they reach the overlay. */
    const val CONFIDENCE_THRESHOLD = 0.50f

    /** Upper bound on simultaneous detections per frame, to cap overlay work. */
    const val MAX_DETECTIONS = 15
}
