package com.securevision.core.domain.usecase.alert

import com.securevision.core.common.dispatcher.DispatcherProvider
import com.securevision.core.domain.repository.AlertRepository
import com.securevision.core.domain.usecase.UseCase
import com.securevision.core.model.AlertRecord
import javax.inject.Inject

/** Persists an alert raised by the detection pipeline. */
class SaveAlertUseCase @Inject constructor(
    private val alertRepository: AlertRepository,
    dispatcherProvider: DispatcherProvider,
) : UseCase<AlertRecord, Unit>(dispatcherProvider.io) {

    override suspend fun execute(parameters: AlertRecord) {
        require(parameters.id.isNotBlank()) { "alert id must not be blank" }

        alertRepository.save(parameters)
    }
}
