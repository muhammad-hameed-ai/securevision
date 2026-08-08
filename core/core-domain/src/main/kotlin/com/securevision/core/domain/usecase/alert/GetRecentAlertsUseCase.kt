package com.securevision.core.domain.usecase.alert

import com.securevision.core.common.dispatcher.DispatcherProvider
import com.securevision.core.domain.repository.AlertRepository
import com.securevision.core.domain.usecase.FlowUseCase
import com.securevision.core.model.AlertRecord
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Streams a bounded window of the newest alerts, for the dashboard preview.
 *
 * Distinct from [GetAlertsUseCase], which streams the whole list for the Alerts
 * screen. The limit is applied by the query, so this stays cheap no matter how
 * much history has accumulated.
 */
class GetRecentAlertsUseCase @Inject constructor(
    private val alertRepository: AlertRepository,
    dispatcherProvider: DispatcherProvider,
) : FlowUseCase<GetRecentAlertsUseCase.Params, List<AlertRecord>>(dispatcherProvider.io) {

    /**
     * @property limit Maximum number of alerts to emit; must be positive.
     */
    data class Params(val limit: Int = DEFAULT_LIMIT)

    override fun execute(parameters: Params): Flow<List<AlertRecord>> {
        require(parameters.limit > 0) { "limit must be positive, was ${parameters.limit}" }

        return alertRepository.getRecent(parameters.limit)
    }

    companion object {
        /** How many alerts the dashboard previews. */
        const val DEFAULT_LIMIT = 5
    }
}
