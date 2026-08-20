package com.securevision.core.domain.usecase.profile

import com.securevision.core.common.dispatcher.DispatcherProvider
import com.securevision.core.domain.repository.EnrolledProfileRepository
import com.securevision.core.domain.usecase.UseCase
import com.securevision.core.model.AccessLevel
import com.securevision.core.model.EnrolledProfile
import javax.inject.Inject

/**
 * Edits a profile's details without touching its face.
 *
 * Correcting a spelling must not require standing in front of the camera again.
 * The embedding and photo are carried across untouched — this use case has no
 * access to the embedder at all, which is what guarantees an edit can never
 * silently alter what the app recognises.
 *
 * Re-enrolling a face is the other operation, and it goes through
 * [com.securevision.core.domain.usecase.live.EnrolFaceFromFrameUseCase] with
 * `replacingProfileId` set.
 */
class UpdateProfileUseCase @Inject constructor(
    private val repository: EnrolledProfileRepository,
    dispatcherProvider: DispatcherProvider,
) : UseCase<UpdateProfileUseCase.Params, EnrolledProfile>(dispatcherProvider.io) {

    /**
     * @property id Which profile to edit.
     * @property name New display name.
     * @property age New age in years.
     * @property accessLevel New operator classification.
     * @property isWatchlisted Whether sightings should be escalated.
     */
    data class Params(
        val id: String,
        val name: String,
        val age: Int,
        val accessLevel: AccessLevel,
        val isWatchlisted: Boolean,
    )

    override suspend fun execute(parameters: Params): EnrolledProfile {
        val existing = repository.getById(parameters.id)
            ?: throw ProfileNotFoundException(parameters.id)

        val updated = existing.copy(
            name = parameters.name.trim(),
            age = parameters.age,
            accessLevel = parameters.accessLevel,
            isWatchlisted = parameters.isWatchlisted,
        )

        // The same rules enrolment enforces. Shared, so an edit cannot produce a
        // profile that enrolment would have rejected.
        ProfileRules.validate(updated)

        repository.add(updated)

        return updated
    }
}

/**
 * Raised when an edit targets a profile that no longer exists.
 *
 * Reachable in normal use: the list is a Flow, so a profile can be deleted on one
 * screen while an edit form for it is still open on the back stack.
 *
 * @property id The missing profile's identifier.
 */
class ProfileNotFoundException(val id: String) : Exception("No profile with id $id")
