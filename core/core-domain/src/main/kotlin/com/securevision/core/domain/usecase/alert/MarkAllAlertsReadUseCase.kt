package com.securevision.core.domain.usecase.alert

import com.securevision.core.common.dispatcher.DispatcherProvider
import com.securevision.core.domain.repository.AlertRepository
import com.securevision.core.domain.usecase.UseCase
import javax.inject.Inject

/**
 * Clears the unread badge by marking every alert read.
 *
 * Marks, never deletes. An operator dismissing the badge is saying "I have seen
 * these", not "these did not happen" — the audit trail has to survive the
 * gesture, and retention pruning is the only thing that removes alerts.
 */
class MarkAllAlertsReadUseCase @Inject constructor(
    private val alertRepository: AlertRepository,
    dispatcherProvider: DispatcherProvider,
) : UseCase<Unit, Unit>(dispatcherProvider.io) {

    override suspend fun execute(parameters: Unit) {
        alertRepository.markAllRead()
    }
}
