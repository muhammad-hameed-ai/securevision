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
        ProfileRules.validate(parameters)
        enrolledProfileRepository.add(parameters)
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

        /**
         * Embedding was absent or implausibly short, meaning inference did not
         * produce a usable vector.
         */
        EMBEDDING_MISSING,

        /** Embedding was entirely zeros, meaning inference ran but produced nothing. */
        EMBEDDING_EMPTY,
    }
}

/** Enrolment rules applied to every profile. */
object ProfileRules {

    /** Ages accepted at enrolment. */
    val AGE_RANGE: IntRange = 1..120

    /**
     * Shortest vector that could plausibly be a face embedding.
     *
     * A floor rather than an exact size. Real models emit 128, 192, 512 or more;
     * anything below this is a bug, not a different model.
     */
    const val MIN_EMBEDDING_SIZE: Int = 64

    /**
     * Applies every enrolment rule.
     *
     * Shared rather than private to [AddEnrolledProfileUseCase] because the live
     * screen's quick-enrolment path must enforce exactly the same rules. Two
     * copies of this logic would eventually disagree, and the one that drifted
     * would be the one letting bad embeddings into the database.
     *
     * @param profile The profile about to be stored.
     * @throws ProfileValidationException with the first rule that failed.
     */
    fun validate(profile: EnrolledProfile) {
        val reason = when {
            profile.name.isBlank() -> ProfileValidationException.Reason.BLANK_NAME

            profile.age !in AGE_RANGE -> ProfileValidationException.Reason.AGE_OUT_OF_RANGE

            profile.photoUri.isBlank() -> ProfileValidationException.Reason.MISSING_PHOTO

            // Dimension is deliberately not pinned to a constant: it is a property
            // of whichever model produced the vector. The engine enforces that all
            // profiles share the active model's dimension at match time, which is
            // where a mismatch actually matters.
            profile.embedding.size < MIN_EMBEDDING_SIZE ->
                ProfileValidationException.Reason.EMBEDDING_MISSING

            profile.embedding.all { it == 0f } -> ProfileValidationException.Reason.EMBEDDING_EMPTY

            else -> null
        }

        if (reason != null) throw ProfileValidationException(reason)
    }
}
