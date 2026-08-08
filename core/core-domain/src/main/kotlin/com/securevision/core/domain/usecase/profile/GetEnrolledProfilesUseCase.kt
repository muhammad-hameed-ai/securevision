package com.securevision.core.domain.usecase.profile

import com.securevision.core.common.dispatcher.DispatcherProvider
import com.securevision.core.domain.repository.EnrolledProfileRepository
import com.securevision.core.domain.usecase.FlowUseCase
import com.securevision.core.model.EnrolledProfile
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Streams the enrolled person profiles, optionally filtered by a search query.
 *
 * Routing the blank-query case to `getAll` keeps the "no filter" path off the
 * search index, so the Profiles screen does not pay for a search it did not ask
 * for on first load.
 */
class GetEnrolledProfilesUseCase @Inject constructor(
    private val enrolledProfileRepository: EnrolledProfileRepository,
    dispatcherProvider: DispatcherProvider,
) : FlowUseCase<GetEnrolledProfilesUseCase.Params, List<EnrolledProfile>>(dispatcherProvider.io) {

    /**
     * @property query Search text; blank yields every profile.
     */
    data class Params(val query: String = "")

    override fun execute(parameters: Params): Flow<List<EnrolledProfile>> {
        val query = parameters.query.trim()

        return if (query.isEmpty()) {
            enrolledProfileRepository.getAll()
        } else {
            enrolledProfileRepository.search(query)
        }
    }
}
