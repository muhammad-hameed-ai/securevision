package com.securevision.core.domain.usecase.alert

import com.securevision.core.common.dispatcher.DispatcherProvider
import com.securevision.core.domain.repository.AlertRepository
import com.securevision.core.domain.usecase.UseCase
import com.securevision.core.model.AlertRecord
import javax.inject.Inject

/**
 * Puts a dismissed alert back.
 *
 * A plain re-insert of the record [DismissAlertUseCase] returned. The alert
 * carries its own id, timestamp and read flag, so it reappears in the same place
 * in the list with the same state — no soft-delete column, and no query anywhere
 * that has to learn to skip tombstones.
 */
class RestoreAlertUseCase @Inject constructor(
    private val alertRepository: AlertRepository,
    dispatcherProvider: DispatcherProvider,
) : UseCase<AlertRecord, Unit>(dispatcherProvider.io) {

    override suspend fun execute(parameters: AlertRecord) {
        alertRepository.save(parameters)
    }
}
