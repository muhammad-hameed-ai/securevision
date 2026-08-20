package com.securevision.feature.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securevision.core.common.result.getOrDefault
import com.securevision.core.common.result.getOrNull
import com.securevision.core.domain.usecase.alert.DismissAlertUseCase
import com.securevision.core.domain.usecase.alert.GetAlertsUseCase
import com.securevision.core.domain.usecase.alert.MarkAllAlertsReadUseCase
import com.securevision.core.domain.usecase.alert.RestoreAlertUseCase
import com.securevision.core.domain.usecase.dashboard.GetUnreadAlertCountUseCase
import com.securevision.core.domain.usecase.invoke
import com.securevision.core.model.AlertRecord
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Drives the alerts gallery.
 *
 * The unread count comes from its own query rather than being counted in the
 * list, so the badge stays correct while a filter is hiding most of the rows.
 */
@HiltViewModel
class AlertsViewModel @Inject constructor(
    getAlerts: GetAlertsUseCase,
    getUnreadCount: GetUnreadAlertCountUseCase,
    private val markAllRead: MarkAllAlertsReadUseCase,
    private val dismissAlert: DismissAlertUseCase,
    private val restoreAlert: RestoreAlertUseCase,
) : ViewModel() {

    private val filter = MutableStateFlow(AlertFilter.ALL)

    private val _lastDismissed = MutableStateFlow<AlertRecord?>(null)

    /** The most recently dismissed alert, offered for undo. */
    val lastDismissed: StateFlow<AlertRecord?> = _lastDismissed.asStateFlow()

    /** Current screen state. */
    val uiState: StateFlow<AlertsUiState> = combine(
        getAlerts(GetAlertsUseCase.Params()).map { result -> result.getOrDefault(emptyList()) },
        getUnreadCount().map { result -> result.getOrDefault(0) },
        filter,
    ) { alerts, unread, activeFilter ->
        if (alerts.isEmpty()) {
            AlertsUiState.Empty
        } else {
            AlertsUiState.Content(
                alerts = alerts.filter(activeFilter::matches),
                unreadCount = unread,
                filter = activeFilter,
                totalCount = alerts.size,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MILLIS),
        initialValue = AlertsUiState.Loading,
    )

    /**
     * Switches the active filter.
     *
     * @param value Which chip was tapped.
     */
    fun onFilterChange(value: AlertFilter) {
        filter.value = value
    }

    /** Clears the unread badge. Marks, never deletes — the audit trail survives. */
    fun onMarkAllRead() {
        viewModelScope.launch { markAllRead() }
    }

    /**
     * Removes one alert, keeping it for undo.
     *
     * @param alert The swiped alert.
     */
    fun onDismiss(alert: AlertRecord) {
        viewModelScope.launch {
            _lastDismissed.value = dismissAlert(DismissAlertUseCase.Params(alert.id)).getOrNull()
        }
    }

    /** Puts the last dismissed alert back exactly as it was. */
    fun onUndoDismiss() {
        val target = _lastDismissed.value ?: return
        _lastDismissed.value = null

        viewModelScope.launch { restoreAlert(target) }
    }

    /** Clears the undo offer once its snackbar has gone. */
    fun onUndoExpired() {
        _lastDismissed.value = null
    }

    private companion object {
        const val SUBSCRIPTION_TIMEOUT_MILLIS = 5_000L
    }
}
