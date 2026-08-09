package com.securevision.core.model

/**
 * Soft attributes inferred from an aligned face crop, reported alongside an alert.
 *
 * Every inference here runs on-device.
 *
 * **Every field is nullable, and `null` means "not assessed" — never "no".** That
 * distinction is load-bearing: a notification claiming someone was not wearing a
 * mask, when in fact no classifier ever looked, is a false statement about a
 * security event. A classifier that is absent, failed to load, or declined on a
 * poor crop all produce `null`, and the UI renders that as unknown.
 *
 * @property age Estimated age in years, or `null` when not assessed.
 * @property gender Estimated gender label, or `null` when not assessed.
 * @property emotion Estimated dominant emotion, or `null` when not assessed.
 * @property hasBeard Whether facial hair was detected, or `null` when not assessed.
 * @property hasMask Whether a face covering was detected, or `null` when not assessed.
 */
data class FaceAttributes(
    val age: Int? = null,
    val gender: String? = null,
    val emotion: String? = null,
    val hasBeard: Boolean? = null,
    val hasMask: Boolean? = null,
) {
    /** `true` when no classifier produced anything, so there is nothing to report. */
    val isEmpty: Boolean
        get() = age == null && gender == null && emotion == null &&
            hasBeard == null && hasMask == null

    companion object {
        /** Nothing was assessed. The state when attribute analysis is switched off. */
        val NOT_ASSESSED = FaceAttributes()
    }
}
