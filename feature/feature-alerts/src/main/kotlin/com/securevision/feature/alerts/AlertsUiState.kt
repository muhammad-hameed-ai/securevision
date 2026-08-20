package com.securevision.feature.alerts

import androidx.compose.runtime.Immutable
import com.securevision.core.model.AlertRecord
import com.securevision.core.model.AlertType
import com.securevision.core.model.Severity

/**
 * Which alerts the gallery is showing.
 *
 * A filter rather than a query parameter, applied over one subscription: the set
 * is small, and re-subscribing per chip tap would restart the Flow and flash the
 * list.
 */
enum class AlertFilter {

    /** Everything, newest first. */
    ALL,

    /** Only alerts the operator has not acknowledged. */
    UNREAD,

    /** Unrecognised people. */
    UNKNOWN,

    /** Weapons. */
    WEAPON,

    /** Movement. */
    MOTION,

    /** People the app recognised — the reassuring half of the log. */
    RECOGNISED,

    /** Anything at [Severity.CRITICAL], whatever raised it. */
    CRITICAL,

    ;

    /**
     * Whether an alert belongs in this filter.
     *
     * @param alert The alert to test.
     */
    fun matches(alert: AlertRecord): Boolean = when (this) {
        ALL -> true
        UNREAD -> !alert.isRead
        UNKNOWN -> alert.type == AlertType.UNKNOWN_PERSON
        WEAPON -> alert.type == AlertType.WEAPON
        MOTION -> alert.type == AlertType.MOTION
        RECOGNISED -> alert.type == AlertType.KNOWN_PERSON
        CRITICAL -> alert.severity.isAtLeast(Severity.CRITICAL)
    }
}

/**
 * What the alerts gallery renders.
 *
 * [Empty] means nothing has ever been recorded — the reassuring "all clear"
 * state. A filter that matches nothing is still [Content], because telling an
 * operator "all clear" while a weapon alert sits one chip away would be a lie.
 */
@Immutable
sealed interface AlertsUiState {

    /** The first load has not produced a list yet. */
    data object Loading : AlertsUiState

    /**
     * Alerts are being shown.
     *
     * @property alerts The filtered list, newest first.
     * @property unreadCount Unread across all alerts, not just the filtered view.
     * @property filter Which chip is active.
     * @property totalCount How many alerts exist before filtering.
     */
    @Immutable
    data class Content(
        val alerts: List<AlertRecord>,
        val unreadCount: Int,
        val filter: AlertFilter = AlertFilter.ALL,
        val totalCount: Int = alerts.size,
    ) : AlertsUiState {

        /** Whether the active filter is hiding everything. */
        val isFilteredEmpty: Boolean get() = alerts.isEmpty() && totalCount > 0
    }

    /** Nothing has been recorded. */
    data object Empty : AlertsUiState
}
