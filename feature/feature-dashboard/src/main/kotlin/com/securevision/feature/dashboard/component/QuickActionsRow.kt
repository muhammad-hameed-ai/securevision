package com.securevision.feature.dashboard.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.securevision.core.ui.preview.PreviewContainer
import com.securevision.core.ui.preview.ThemePreviews
import com.securevision.core.ui.theme.SecureVisionDimens
import com.securevision.feature.dashboard.R

/**
 * Row of shortcuts to the destinations reached most often from the dashboard.
 *
 * @param onLiveClick Opens Live View.
 * @param onAlertsClick Opens the alerts list.
 * @param onProfilesClick Opens the enrolled profiles list.
 * @param onSettingsClick Opens settings.
 * @param modifier Modifier applied to the row.
 */
@Composable
fun QuickActionsRow(
    onLiveClick: () -> Unit,
    onAlertsClick: () -> Unit,
    onProfilesClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(SecureVisionDimens.spacingSmall),
    ) {
        QuickAction(
            icon = Icons.Outlined.Videocam,
            label = stringResource(R.string.dashboard_action_live),
            onClick = onLiveClick,
        )
        QuickAction(
            icon = Icons.Outlined.NotificationsActive,
            label = stringResource(R.string.dashboard_action_alerts),
            onClick = onAlertsClick,
        )
        QuickAction(
            icon = Icons.Outlined.People,
            label = stringResource(R.string.dashboard_action_profiles),
            onClick = onProfilesClick,
        )
        QuickAction(
            icon = Icons.Outlined.Settings,
            label = stringResource(R.string.dashboard_action_settings),
            onClick = onSettingsClick,
        )
    }
}

@Composable
private fun QuickAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(ACTION_WIDTH)
            // clip before background and clickable so the ripple is bounded by the
            // rounded shape rather than spilling into a rectangle.
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = SecureVisionDimens.spacingMediumSmall),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SecureVisionDimens.spacingExtraSmall),
    ) {
        Icon(
            imageVector = icon,
            // Decorative: the label directly beneath names the action.
            contentDescription = null,
            modifier = Modifier.size(SecureVisionDimens.iconMedium),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

private val ACTION_WIDTH = 80.dp

@ThemePreviews
@Composable
private fun QuickActionsRowPreview() {
    PreviewContainer {
        QuickActionsRow(
            onLiveClick = {},
            onAlertsClick = {},
            onProfilesClick = {},
            onSettingsClick = {},
        )
    }
}
