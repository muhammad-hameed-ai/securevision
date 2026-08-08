package com.securevision.feature.dashboard

import androidx.compose.runtime.Immutable
import com.securevision.core.model.AlertRecord

/**
 * Everything the Dashboard needs to render, as one closed hierarchy.
 *
 * There is deliberately no `Empty` case. On a fresh install every count is zero,
 * and that is the normal first-run state, not an absence of state — the screen
 * still shows its three stat cards reading zero. Emptiness is a property of the
 * recent-alerts list inside [Content], handled where it actually applies.
 */
@Immutable
sealed interface DashboardUiState {

    /** No source has emitted yet. */
    data object Loading : DashboardUiState

    /**
     * Live figures from the on-device database.
     *
     * @property unreadAlerts Alerts the user has not opened.
     * @property profileCount People the app can recognise.
     * @property eventCount Total detections recorded.
     * @property recentAlerts Newest alerts, bounded; empty on a fresh install.
     */
    @Immutable
    data class Content(
        val unreadAlerts: Int,
        val profileCount: Int,
        val eventCount: Int,
        val recentAlerts: List<AlertRecord>,
    ) : DashboardUiState {

        /** Whether the recent-alerts section should render its empty state. */
        val hasNoAlerts: Boolean get() = recentAlerts.isEmpty()
    }

    /**
     * A source failed.
     *
     * Present so a failed database read surfaces instead of leaving the screen on
     * [Loading] forever, which is indistinguishable from a hang.
     *
     * @property message Cause, when one was reported.
     */
    data class Error(val message: String?) : DashboardUiState
}
