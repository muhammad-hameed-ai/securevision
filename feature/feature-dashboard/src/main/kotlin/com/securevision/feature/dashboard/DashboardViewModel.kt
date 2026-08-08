package com.securevision.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securevision.core.common.extension.stateInWhileSubscribed
import com.securevision.core.common.result.Result
import com.securevision.core.common.result.getOrDefault
import com.securevision.core.domain.usecase.alert.GetRecentAlertsUseCase
import com.securevision.core.domain.usecase.dashboard.GetDetectionEventCountUseCase
import com.securevision.core.domain.usecase.dashboard.GetEnrolledProfileCountUseCase
import com.securevision.core.domain.usecase.dashboard.GetUnreadAlertCountUseCase
import com.securevision.core.domain.usecase.invoke
import com.securevision.core.model.AlertRecord
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine

/**
 * Collects the four dashboard figures into one state.
 *
 * Reaches the data layer only through domain use cases — `feature-dashboard`
 * declares no dependency on `core-data`, so a repository is not even on this
 * module's compile classpath.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    getUnreadAlertCount: GetUnreadAlertCountUseCase,
    getEnrolledProfileCount: GetEnrolledProfileCountUseCase,
    getDetectionEventCount: GetDetectionEventCountUseCase,
    getRecentAlerts: GetRecentAlertsUseCase,
) : ViewModel() {

    /** Current dashboard state. */
    val uiState: StateFlow<DashboardUiState> = combine(
        getUnreadAlertCount(),
        getEnrolledProfileCount(),
        getDetectionEventCount(),
        getRecentAlerts(GetRecentAlertsUseCase.Params()),
        ::toUiState,
    ).stateInWhileSubscribed(
        scope = viewModelScope,
        initialValue = DashboardUiState.Loading,
    )
}

/**
 * Reduces four independent [Result] streams into one screen state.
 *
 * Precedence is error first, then loading. A partial dashboard — three real
 * numbers beside one that silently defaulted to zero because its query failed —
 * would be worse than no dashboard, because zero is a plausible value here and
 * the user could not tell the difference.
 */
private fun toUiState(
    unreadAlerts: Result<Int>,
    profileCount: Result<Int>,
    eventCount: Result<Int>,
    recentAlerts: Result<List<AlertRecord>>,
): DashboardUiState {
    val sources = listOf(unreadAlerts, profileCount, eventCount, recentAlerts)

    sources.filterIsInstance<Result.Error>().firstOrNull()?.let { failure ->
        return DashboardUiState.Error(failure.message)
    }

    if (sources.any { it is Result.Loading }) {
        return DashboardUiState.Loading
    }

    return DashboardUiState.Content(
        unreadAlerts = unreadAlerts.getOrDefault(0),
        profileCount = profileCount.getOrDefault(0),
        eventCount = eventCount.getOrDefault(0),
        recentAlerts = recentAlerts.getOrDefault(emptyList()),
    )
}
