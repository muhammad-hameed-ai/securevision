package com.securevision.core.domain.usecase.profile

import com.securevision.core.common.dispatcher.DispatcherProvider
import com.securevision.core.domain.repository.EnrolledProfileRepository
import com.securevision.core.domain.usecase.UseCase
import com.securevision.core.model.EnrolledProfile
import javax.inject.Inject

/**
 * Enrols a person so the live pipeline can recognise them.
 *
 * The embedding checks here are load-bearing for recognition accuracy: an
 * embedding of the wrong length means the wrong model produced it, and an
 * all-zero embedding means the face crop never reached the model. Both would
 * otherwise surface much later as inexplicably poor match scores.
 */
class AddEnrolledProfileUseCase @Inject constructor(
    private val enrolledProfileRepository: EnrolledProfileRepository,
    dispatcherProvider: DispatcherProvider,
) : UseCase<EnrolledProfile, Unit>(dispatcherProvider.io) {

    override suspend fun execute(parameters: EnrolledProfile) {
        validate(parameters)
        enrolledProfileRepository.add(parameters)
    }

    private fun validate(profile: EnrolledProfile) {
        when {
            profile.name.isBlank() ->
                throw ProfileValidationException(ProfileValidationException.Reason.BLANK_NAME)

            profile.age !in ProfileRules.AGE_RANGE ->
                throw ProfileValidationException(ProfileValidationException.Reason.AGE_OUT_OF_RANGE)

            profile.photoUri.isBlank() ->
                throw ProfileValidationException(ProfileValidationException.Reason.MISSING_PHOTO)

            profile.embedding.size != EnrolledProfile.EMBEDDING_SIZE ->
                throw ProfileValidationException(ProfileValidationException.Reason.EMBEDDING_WRONG_SIZE)

            profile.embedding.all { it == 0f } ->
                throw ProfileValidationException(ProfileValidationException.Reason.EMBEDDING_EMPTY)
        }
    }
}

/**
 * Raised when a profile fails a domain rule before it is persisted.
 *
 * Carries a [reason] rather than a display message so the presentation layer can
 * localise it.
 *
 * @property reason Which rule was broken.
 */
class ProfileValidationException(val reason: Reason) : Exception(reason.name) {

    /** The enrolment rules. */
    enum class Reason {
        /** Name was empty or whitespace. */
        BLANK_NAME,

        /** Age fell outside [ProfileRules.AGE_RANGE]. */
        AGE_OUT_OF_RANGE,

        /** No enrolment photo was captured. */
        MISSING_PHOTO,

        /** Embedding length did not match [EnrolledProfile.EMBEDDING_SIZE]. */
        EMBEDDING_WRONG_SIZE,

        /** Embedding was entirely zeros, meaning inference never ran. */
        EMBEDDING_EMPTY,
    }
}

/** Enrolment rules applied to every profile. */
object ProfileRules {

    /** Ages accepted at enrolment. */
    val AGE_RANGE: IntRange = 1..120
}
