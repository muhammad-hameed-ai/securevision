package com.securevision.core.domain.usecase.dashboard

import com.securevision.core.common.dispatcher.DispatcherProvider
import com.securevision.core.domain.repository.AlertRepository
import com.securevision.core.domain.usecase.FlowUseCase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Streams the number of alerts the user has not yet read. */
class GetUnreadAlertCountUseCase @Inject constructor(
    private val alertRepository: AlertRepository,
    dispatcherProvider: DispatcherProvider,
) : FlowUseCase<Unit, Int>(dispatcherProvider.io) {

    override fun execute(parameters: Unit): Flow<Int> = alertRepository.getUnreadCount()
}
