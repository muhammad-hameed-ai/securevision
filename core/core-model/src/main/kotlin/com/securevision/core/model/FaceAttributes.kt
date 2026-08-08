package com.securevision.core.model

/**
 * Soft attributes inferred from a detected face, reported alongside an alert.
 *
 * Every inference here runs on-device. The nullable fields are attributes the
 * classifier may decline to report on a low-quality crop; [hasBeard] and
 * [hasMask] always carry a verdict because they are what the notification text
 * is required to state.
 *
 * @property age Estimated age in years, or `null` when not estimated.
 * @property gender Estimated gender label, or `null` when not estimated.
 * @property emotion Estimated dominant emotion, or `null` when not estimated.
 * @property hasBeard Whether facial hair was detected.
 * @property hasMask Whether a face covering was detected.
 */
data class FaceAttributes(
    val age: Int?,
    val gender: String?,
    val emotion: String?,
    val hasBeard: Boolean,
    val hasMask: Boolean,
)
