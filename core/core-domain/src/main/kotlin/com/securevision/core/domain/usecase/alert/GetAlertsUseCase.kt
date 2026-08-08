package com.securevision.core.domain.usecase.alert

import com.securevision.core.common.dispatcher.DispatcherProvider
import com.securevision.core.domain.repository.AlertRepository
import com.securevision.core.domain.usecase.FlowUseCase
import com.securevision.core.model.AlertRecord
import com.securevision.core.model.AlertType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Streams stored alerts, newest first, optionally narrowed to one category. */
class GetAlertsUseCase @Inject constructor(
    private val alertRepository: AlertRepository,
    dispatcherProvider: DispatcherProvider,
) : FlowUseCase<GetAlertsUseCase.Params, List<AlertRecord>>(dispatcherProvider.io) {

    /**
     * @property type Category to filter by; `null` yields every alert.
     */
    data class Params(val type: AlertType? = null)

    override fun execute(parameters: Params): Flow<List<AlertRecord>> =
        when (val type = parameters.type) {
            null -> alertRepository.getAll()
            else -> alertRepository.getByType(type)
        }
}
