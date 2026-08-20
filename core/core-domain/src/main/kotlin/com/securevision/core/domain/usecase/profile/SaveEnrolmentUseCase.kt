package com.securevision.core.domain.usecase.profile

import com.securevision.core.common.dispatcher.DispatcherProvider
import com.securevision.core.domain.engine.EnrolmentCapture
import com.securevision.core.domain.engine.ProfilePhotoStore
import com.securevision.core.domain.repository.EnrolledProfileRepository
import com.securevision.core.domain.usecase.UseCase
import com.securevision.core.model.AccessLevel
import com.securevision.core.model.EnrolledProfile
import java.util.UUID
import javax.inject.Inject

/**
 * Commits a validated capture as an enrolled person.
 *
 * Takes an [EnrolmentCapture.Success] the operator has already seen and accepted,
 * so this step cannot fail on face quality — that verdict was reached in
 * [CaptureEnrolmentUseCase]. This use case holds no reference to the recognition
 * engine at all, which is what structurally prevents a second embedding path from
 * appearing here.
 */
class SaveEnrolmentUseCase @Inject constructor(
    private val photoStore: ProfilePhotoStore,
    private val repository: EnrolledProfileRepository,
    dispatcherProvider: DispatcherProvider,
) : UseCase<SaveEnrolmentUseCase.Params, EnrolledProfile>(dispatcherProvider.io) {

    /**
     * @property capture The accepted capture, carrying the aligned crop and its embedding.
     * @property name Display name shown when this person is recognised.
     * @property age Age in years.
     * @property accessLevel Operator classification.
     * @property isWatchlisted Whether sightings should be escalated.
     * @property replacingProfileId Set when re-enrolling an existing person. The
     *   profile keeps its id and enrolment date, so history already pointing at
     *   them stays attached instead of being orphaned by a delete-and-recreate.
     */
    data class Params(
        val capture: EnrolmentCapture.Success,
        val name: String,
        val age: Int,
        val accessLevel: AccessLevel = AccessLevel.DEFAULT,
        val isWatchlisted: Boolean = false,
        val replacingProfileId: String? = null,
    )

    override suspend fun execute(parameters: Params): EnrolledProfile {
        val existing = parameters.replacingProfileId?.let { id -> repository.getById(id) }

        // The stored photo is the aligned crop, not the wider frame: it shows
        // exactly what the model embedded, which makes a bad enrolment obvious by
        // looking at it rather than only by its match scores.
        val photoUri = photoStore.saveProfilePhoto(parameters.capture.alignedFace)

        val profile = EnrolledProfile(
            id = existing?.id ?: UUID.randomUUID().toString(),
            name = parameters.name.trim(),
            age = parameters.age,
            photoUri = photoUri,
            embedding = parameters.capture.embedding,
            accessLevel = parameters.accessLevel,
            isWatchlisted = parameters.isWatchlisted,
            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
        )

        ProfileRules.validate(profile)

        repository.add(profile)

        // Only after the replacement row is committed. Deleting first would leave
        // the person with no photo if the write failed.
        existing?.takeIf { old -> old.photoUri != photoUri }
            ?.let { old -> photoStore.deleteProfilePhoto(old.photoUri) }

        return profile
    }
}
