package com.securevision.core.domain.usecase.alert

import com.securevision.core.common.dispatcher.DispatcherProvider
import com.securevision.core.domain.repository.AlertRepository
import com.securevision.core.domain.usecase.UseCase
import com.securevision.core.model.AlertRecord
import javax.inject.Inject

/**
 * Removes one alert, returning it so the action can be undone.
 *
 * The removed record is handed back rather than kept in a hidden buffer, because
 * the undo has to survive the ViewModel: a snackbar can outlive a configuration
 * change, and an undo that quietly stopped working after a rotation would be
 * worse than offering none. Restoring is [RestoreAlertUseCase] with this record.
 *
 * Returns `null` when the alert is already gone — dismissing twice is not an
 * error, it is two taps racing.
 */
class DismissAlertUseCase @Inject constructor(
    private val alertRepository: AlertRepository,
    dispatcherProvider: DispatcherProvider,
) : UseCase<DismissAlertUseCase.Params, AlertRecord?>(dispatcherProvider.io) {

    /**
     * @property alertId Identifier of the alert to remove.
     */
    data class Params(val alertId: String)

    override suspend fun execute(parameters: Params): AlertRecord? {
        require(parameters.alertId.isNotBlank()) { "alertId must not be blank" }

        val existing = alertRepository.getById(parameters.alertId) ?: return null
        alertRepository.delete(parameters.alertId)

        return existing
    }
}
