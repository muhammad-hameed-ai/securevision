package com.securevision.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.securevision.core.model.AlertRecord
import com.securevision.core.model.AlertType
import com.securevision.core.model.Severity
import com.securevision.core.ui.component.SVEmptyState
import com.securevision.core.ui.component.SVPrimaryButton
import com.securevision.core.ui.component.SVStatCard
import com.securevision.core.ui.preview.PreviewContainer
import com.securevision.core.ui.preview.ThemePreviews
import com.securevision.core.ui.theme.SecureVisionDimens
import com.securevision.core.ui.theme.SecureVisionTheme
import com.securevision.feature.dashboard.component.QuickActionsRow
import com.securevision.feature.dashboard.component.RecentAlertRow
import com.securevision.feature.dashboard.component.SystemStatusCard

/**
 * The Dashboard, rendered from state alone.
 *
 * Stateless by design: it takes a [DashboardUiState] and navigation callbacks, so
 * it previews without Hilt and this module never learns the app's navigation
 * graph. `DashboardRoute` is the Hilt-aware wrapper that supplies both.
 *
 * @param uiState What to render.
 * @param onNavigateToLive Opens Live View.
 * @param onNavigateToAlerts Opens the alerts list.
 * @param onNavigateToProfiles Opens the enrolled profiles list.
 * @param onNavigateToHistory Opens the detection history.
 * @param onNavigateToSettings Opens settings.
 * @param modifier Modifier applied to the screen.
 */
@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    onNavigateToLive: () -> Unit,
    onNavigateToAlerts: () -> Unit,
    onNavigateToProfiles: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        DashboardUiState.Loading -> LoadingState(modifier)

        is DashboardUiState.Error -> SVEmptyState(
            icon = Icons.Outlined.CloudOff,
            title = stringResource(R.string.dashboard_error_title),
            subtitle = stringResource(R.string.dashboard_error_subtitle),
            modifier = modifier.fillMaxSize(),
        )

        is DashboardUiState.Content -> ContentState(
            content = uiState,
            onNavigateToLive = onNavigateToLive,
            onNavigateToAlerts = onNavigateToAlerts,
            onNavigateToProfiles = onNavigateToProfiles,
            onNavigateToHistory = onNavigateToHistory,
            onNavigateToSettings = onNavigateToSettings,
            modifier = modifier,
        )
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun ContentState(
    content: DashboardUiState.Content,
    onNavigateToLive: () -> Unit,
    onNavigateToAlerts: () -> Unit,
    onNavigateToProfiles: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // The button is pinned and only the alert list scrolls. Previously the whole
    // screen was one scrolling Column, so the primary action — the one thing an
    // operator reaches for in a hurry — slid off the bottom as history grew.
    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(
                start = SecureVisionDimens.spacingMedium,
                end = SecureVisionDimens.spacingMedium,
                top = SecureVisionDimens.spacingMedium,
            ),
            verticalArrangement = Arrangement.spacedBy(SecureVisionDimens.spacingMedium),
        ) {
            SystemStatusCard()

            StatCards(
                content = content,
                onAlertsClick = onNavigateToAlerts,
                onProfilesClick = onNavigateToProfiles,
                onEventsClick = onNavigateToHistory,
            )

            SectionHeading(text = stringResource(R.string.dashboard_quick_actions))

            QuickActionsRow(
                onLiveClick = onNavigateToLive,
                onAlertsClick = onNavigateToAlerts,
                onProfilesClick = onNavigateToProfiles,
                onSettingsClick = onNavigateToSettings,
            )

            SectionHeading(text = stringResource(R.string.dashboard_recent_alerts))
        }

        // weight(1f) hands the list every pixel the fixed content does not use,
        // and lazy composition means a hundred alerts do not all compose to show
        // the five that fit.
        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
            if (content.hasNoAlerts) {
                SVEmptyState(
                    icon = Icons.Outlined.NotificationsOff,
                    title = stringResource(R.string.dashboard_recent_alerts_empty_title),
                    subtitle = stringResource(R.string.dashboard_recent_alerts_empty_subtitle),
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        horizontal = SecureVisionDimens.spacingMedium,
                        vertical = SecureVisionDimens.spacingSmall,
                    ),
                    verticalArrangement = Arrangement.spacedBy(SecureVisionDimens.spacingSmall),
                ) {
                    items(items = content.recentAlerts, key = AlertRecord::id) { alert ->
                        RecentAlertRow(alert = alert, onClick = onNavigateToAlerts)
                    }
                }
            }
        }

        SVPrimaryButton(
            text = stringResource(R.string.dashboard_open_live_view),
            onClick = onNavigateToLive,
            modifier = Modifier
                .fillMaxWidth()
                .padding(SecureVisionDimens.spacingMedium),
        )
    }
}

/**
 * The three headline figures.
 *
 * Laid out with `weight(1f)` rather than a fixed width so the row stays balanced
 * when a count grows to three digits.
 */
@Composable
private fun StatCards(
    content: DashboardUiState.Content,
    onAlertsClick: () -> Unit,
    onProfilesClick: () -> Unit,
    onEventsClick: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(SecureVisionDimens.spacingSmall)) {
        SVStatCard(
            icon = Icons.Outlined.NotificationsActive,
            value = content.unreadAlerts.toString(),
            label = stringResource(R.string.dashboard_stat_alerts),
            modifier = Modifier.weight(1f),
            accentColor = SecureVisionTheme.colors.unknown,
            onClick = onAlertsClick,
        )
        SVStatCard(
            icon = Icons.Outlined.People,
            value = content.profileCount.toString(),
            label = stringResource(R.string.dashboard_stat_profiles),
            modifier = Modifier.weight(1f),
            accentColor = SecureVisionTheme.colors.known,
            onClick = onProfilesClick,
        )
        SVStatCard(
            icon = Icons.Outlined.History,
            value = content.eventCount.toString(),
            label = stringResource(R.string.dashboard_stat_events),
            modifier = Modifier.weight(1f),
            onClick = onEventsClick,
        )
    }
}

@Composable
private fun SectionHeading(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    )
}

@ThemePreviews
@Composable
private fun DashboardScreenEmptyPreview() {
    PreviewContainer {
        DashboardScreen(
            uiState = DashboardUiState.Content(
                unreadAlerts = 0,
                profileCount = 0,
                eventCount = 0,
                recentAlerts = emptyList(),
            ),
            onNavigateToLive = {},
            onNavigateToAlerts = {},
            onNavigateToProfiles = {},
            onNavigateToHistory = {},
            onNavigateToSettings = {},
        )
    }
}

@ThemePreviews
@Composable
private fun DashboardScreenPopulatedPreview() {
    PreviewContainer {
        DashboardScreen(
            uiState = DashboardUiState.Content(
                unreadAlerts = 3,
                profileCount = 12,
                eventCount = 148,
                recentAlerts = listOf(
                    AlertRecord(
                        id = "a1",
                        type = AlertType.UNKNOWN_PERSON,
                        severity = Severity.CRITICAL,
                        confidence = 0.91f,
                        cameraFacing = "front",
                        snapshotUri = null,
                        hasBeard = true,
                        hasMask = false,
                        timestamp = 1_754_000_000_000L,
                        isRead = false,
                    ),
                ),
            ),
            onNavigateToLive = {},
            onNavigateToAlerts = {},
            onNavigateToProfiles = {},
            onNavigateToHistory = {},
            onNavigateToSettings = {},
        )
    }
}
