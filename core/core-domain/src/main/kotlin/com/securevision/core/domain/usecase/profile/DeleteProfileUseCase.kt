package com.securevision.core.domain.usecase.profile

import com.securevision.core.common.dispatcher.DispatcherProvider
import com.securevision.core.domain.repository.EnrolledProfileRepository
import com.securevision.core.domain.usecase.UseCase
import javax.inject.Inject

/** Removes an enrolled person profile and its on-device enrolment photo. */
class DeleteProfileUseCase @Inject constructor(
    private val enrolledProfileRepository: EnrolledProfileRepository,
    dispatcherProvider: DispatcherProvider,
) : UseCase<DeleteProfileUseCase.Params, Unit>(dispatcherProvider.io) {

    /**
     * @property profileId Identifier of the profile to remove.
     */
    data class Params(val profileId: String)

    override suspend fun execute(parameters: Params) {
        require(parameters.profileId.isNotBlank()) { "profileId must not be blank" }

        enrolledProfileRepository.delete(parameters.profileId)
    }
}
