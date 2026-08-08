package com.securevision.core.domain.usecase.dashboard

import com.securevision.core.common.dispatcher.DispatcherProvider
import com.securevision.core.domain.repository.EnrolledProfileRepository
import com.securevision.core.domain.usecase.FlowUseCase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Streams how many people the app can currently recognise.
 *
 * Backed by a `COUNT(*)` query rather than the profile list, so the dashboard
 * never loads a 512-float embedding per person just to display a number.
 */
class GetEnrolledProfileCountUseCase @Inject constructor(
    private val enrolledProfileRepository: EnrolledProfileRepository,
    dispatcherProvider: DispatcherProvider,
) : FlowUseCase<Unit, Int>(dispatcherProvider.io) {

    override fun execute(parameters: Unit): Flow<Int> = enrolledProfileRepository.countAll()
}
