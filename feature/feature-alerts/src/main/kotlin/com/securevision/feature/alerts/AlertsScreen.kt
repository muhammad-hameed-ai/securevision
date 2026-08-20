package com.securevision.feature.alerts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.FilterListOff
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.securevision.core.model.AlertRecord
import com.securevision.core.ui.component.SVChip
import com.securevision.core.ui.component.SVEmptyState
import com.securevision.core.ui.component.SVTopBar
import com.securevision.core.ui.theme.SecureVisionDimens
import com.securevision.feature.alerts.component.AlertCard

/**
 * The alerts gallery.
 *
 * @param uiState What to render.
 * @param lastDismissed Alert offered for undo, if any.
 * @param onFilterChange Filter chip tapped.
 * @param onMarkAllRead Clears the unread badge.
 * @param onDismiss Removes an alert.
 * @param onUndoDismiss Restores the last removed alert.
 * @param onUndoExpired The undo offer has lapsed.
 * @param onOpenAlert Opens the detail view.
 * @param modifier Modifier applied to the screen.
 */
@Composable
fun AlertsScreen(
    uiState: AlertsUiState,
    lastDismissed: AlertRecord?,
    onFilterChange: (AlertFilter) -> Unit,
    onMarkAllRead: () -> Unit,
    onDismiss: (AlertRecord) -> Unit,
    onUndoDismiss: () -> Unit,
    onUndoExpired: () -> Unit,
    onOpenAlert: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val undoLabel = stringResource(R.string.alerts_undo)
    val dismissedMessage = stringResource(R.string.alerts_dismissed)

    LaunchedEffect(lastDismissed) {
        if (lastDismissed == null) return@LaunchedEffect

        val result = snackbarHostState.showSnackbar(
            message = dismissedMessage,
            actionLabel = undoLabel,
        )

        if (result == SnackbarResult.ActionPerformed) onUndoDismiss() else onUndoExpired()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            SVTopBar(
                title = stringResource(R.string.alerts_title),
                actions = {
                    val unread = (uiState as? AlertsUiState.Content)?.unreadCount ?: 0

                    if (unread > 0) {
                        IconButton(onClick = onMarkAllRead) {
                            Icon(
                                imageVector = Icons.Outlined.DoneAll,
                                contentDescription = stringResource(R.string.alerts_mark_all_read),
                            )
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (uiState) {
                AlertsUiState.Loading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary,
                )

                AlertsUiState.Empty -> SVEmptyState(
                    icon = Icons.Outlined.VerifiedUser,
                    title = stringResource(R.string.alerts_empty_title),
                    subtitle = stringResource(R.string.alerts_empty_subtitle),
                    modifier = Modifier.fillMaxSize(),
                )

                is AlertsUiState.Content -> AlertsContent(
                    state = uiState,
                    onFilterChange = onFilterChange,
                    onDismiss = onDismiss,
                    onOpenAlert = onOpenAlert,
                )
            }
        }
    }
}

@Composable
private fun AlertsContent(
    state: AlertsUiState.Content,
    onFilterChange: (AlertFilter) -> Unit,
    onDismiss: (AlertRecord) -> Unit,
    onOpenAlert: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(
                    horizontal = SecureVisionDimens.spacingMedium,
                    vertical = SecureVisionDimens.spacingSmall,
                ),
            horizontalArrangement = Arrangement.spacedBy(SecureVisionDimens.spacingSmall),
        ) {
            AlertFilter.entries.forEach { entry ->
                SVChip(
                    label = stringResource(entry.labelRes()),
                    selected = state.filter == entry,
                    onClick = { onFilterChange(entry) },
                )
            }
        }

        if (state.isFilteredEmpty) {
            SVEmptyState(
                icon = Icons.Outlined.FilterListOff,
                title = stringResource(R.string.alerts_no_matches_title),
                subtitle = stringResource(R.string.alerts_no_matches_subtitle),
                modifier = Modifier.fillMaxSize(),
            )
            return@Column
        }

        LazyColumn(
            contentPadding = PaddingValues(
                start = SecureVisionDimens.spacingMedium,
                end = SecureVisionDimens.spacingMedium,
                bottom = SecureVisionDimens.spacingLarge,
            ),
            verticalArrangement = Arrangement.spacedBy(SecureVisionDimens.spacingSmall),
        ) {
            items(items = state.alerts, key = AlertRecord::id) { alert ->
                SwipeableAlert(
                    alert = alert,
                    onDismiss = { onDismiss(alert) },
                    onClick = { onOpenAlert(alert.id) },
                )
            }
        }
    }
}

@Composable
private fun SwipeableAlert(
    alert: AlertRecord,
    onDismiss: () -> Unit,
    onClick: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            // Only act on a completed swipe. Returning true for the settling
            // states would fire the dismissal twice.
            if (value == SwipeToDismissBoxValue.EndToStart ||
                value == SwipeToDismissBoxValue.StartToEnd
            ) {
                onDismiss()
                true
            } else {
                false
            }
        },
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {},
    ) {
        AlertCard(alert = alert, onClick = onClick)
    }
}

/** Chip copy for each filter. */
internal fun AlertFilter.labelRes(): Int = when (this) {
    AlertFilter.ALL -> R.string.alerts_filter_all
    AlertFilter.UNREAD -> R.string.alerts_filter_unread
    AlertFilter.UNKNOWN -> R.string.alerts_filter_unknown
    AlertFilter.WEAPON -> R.string.alerts_filter_weapon
    AlertFilter.MOTION -> R.string.alerts_filter_motion
    AlertFilter.RECOGNISED -> R.string.alerts_filter_recognised
    AlertFilter.CRITICAL -> R.string.alerts_filter_critical
}
