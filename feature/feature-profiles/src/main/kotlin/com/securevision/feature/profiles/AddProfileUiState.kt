package com.securevision.feature.profiles

import android.graphics.Bitmap
import androidx.compose.runtime.Immutable
import com.securevision.core.domain.engine.EnrolmentCapture
import com.securevision.core.model.AccessLevel

/**
 * The enrolment form.
 *
 * The capture and the details are held separately because they are validated at
 * different moments: a face is accepted or rejected the instant the shutter
 * fires, while the name and age are checked on save. Merging them would either
 * delay the face verdict — the thing the operator most needs to see immediately —
 * or force a name to be typed before the camera would tell them the shot was
 * unusable.
 *
 * @property name Display name being entered.
 * @property age Age being entered, as text so a partially typed value is legal.
 * @property accessLevel Selected classification.
 * @property isWatchlisted Whether sightings should be escalated.
 * @property alignedCrop The accepted crop, shown so a bad enrolment is visible
 *   before it is saved. `null` until a capture succeeds.
 * @property captureFailure Why the last capture was rejected, if it was.
 * @property isPreparing Whether the face model is still loading. Distinct from
 *   [modelUnavailable]: "not ready yet" and "will never be ready" call for very
 *   different things to be said to someone standing in front of a camera.
 * @property modelUnavailable Whether the face model was asked for and genuinely
 *   failed to load.
 * @property isCapturing Whether a shutter press is in flight.
 * @property isSaving Whether the profile is being written.
 * @property nameError What is wrong with the name, if anything.
 * @property ageError What is wrong with the age, if anything.
 * @property isEditing Whether this is a re-enrolment of an existing person.
 */
@Immutable
data class AddProfileUiState(
    val name: String = "",
    val age: String = "",
    val accessLevel: AccessLevel = AccessLevel.DEFAULT,
    val isWatchlisted: Boolean = false,
    val alignedCrop: Bitmap? = null,
    val captureFailure: EnrolmentCapture.Failure.Reason? = null,
    val isPreparing: Boolean = false,
    val modelUnavailable: Boolean = false,
    val isCapturing: Boolean = false,
    val isSaving: Boolean = false,
    val nameError: FieldError? = null,
    val ageError: FieldError? = null,
    val isEditing: Boolean = false,
) {

    /** Whether a usable face has been captured. */
    val hasFace: Boolean get() = alignedCrop != null

    /**
     * Whether save should be offered.
     *
     * A face is mandatory: a profile without an embedding could never be matched,
     * so allowing one would create a row that silently does nothing.
     */
    val canSave: Boolean
        get() = hasFace && name.isNotBlank() && age.isNotBlank() && !isSaving && !isCapturing

    /** Whether the shutter should be offered at all. */
    val canCapture: Boolean get() = !isPreparing && !modelUnavailable && !isCapturing
}

/**
 * A field-level validation failure.
 *
 * An enum rather than a message string: the ViewModel has no business holding
 * user-facing copy, and a localised app cannot have English baked into state.
 * The screen maps these onto string resources.
 */
enum class FieldError {

    /** The field was left empty. */
    REQUIRED,

    /** An age outside the range enrolment accepts. */
    AGE_OUT_OF_RANGE,
}
