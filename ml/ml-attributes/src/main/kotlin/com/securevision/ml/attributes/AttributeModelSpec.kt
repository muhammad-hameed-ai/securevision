package com.securevision.ml.attributes

/**
 * Provisional metadata for the soft-attribute classifiers, implemented in
 * Phase 5. These produce [com.securevision.core.model.FaceAttributes].
 *
 * Beard and mask are reported in every face notification, so their classifiers
 * always run. Age, gender and emotion are best-effort and may decline to answer
 * on a low-quality crop.
 */
internal object AttributeModelSpec {

    /** Asset name of the beard/mask classifier. */
    const val COVERING_ASSET_NAME = "face_covering.tflite"

    /** Asset name of the age and gender estimator. */
    const val DEMOGRAPHIC_ASSET_NAME = "age_gender.tflite"

    /** Asset name of the emotion classifier. */
    const val EMOTION_ASSET_NAME = "emotion.tflite"

    /** Square input edge, in pixels; matches the aligned crop from :ml:ml-face. */
    const val INPUT_SIZE = 160

    /** Below this score, an optional attribute is reported as `null` rather than guessed. */
    const val OPTIONAL_ATTRIBUTE_THRESHOLD = 0.55f
}
