package com.securevision.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.securevision.core.ui.component.SVEmptyState
import com.securevision.core.ui.component.SVPrimaryButton
import com.securevision.core.ui.preview.PreviewContainer
import com.securevision.core.ui.preview.ThemePreviews
import com.securevision.core.ui.theme.SecureVisionDimens

/**
 * Stand-in for a screen that a later phase implements.
 *
 * Renders through the real design-system components rather than raw text, so the
 * theme, spacing and typography are exercised on every destination from Phase 1
 * onward — a placeholder built from `Text("Dashboard")` would prove nothing.
 *
 * @param icon Icon representing the destination.
 * @param title Destination name.
 * @param description What this screen will do, and when it is built.
 * @param modifier Modifier applied to the layout.
 * @param actionLabel Label for an optional primary action.
 * @param onAction Invoked when the primary action is pressed.
 */
@Composable
fun PlaceholderScreen(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = SecureVisionDimens.spacingMedium),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SVEmptyState(
            icon = icon,
            title = title,
            subtitle = description,
        )

        if (actionLabel != null && onAction != null) {
            SVPrimaryButton(
                text = actionLabel,
                onClick = onAction,
                modifier = Modifier.widthIn(max = MAX_ACTION_WIDTH),
            )
        }
    }
}

private val MAX_ACTION_WIDTH = 360.dp

@ThemePreviews
@Composable
private fun PlaceholderScreenPreview() {
    PreviewContainer {
        PlaceholderScreen(
            icon = Icons.Outlined.Dashboard,
            title = "Dashboard",
            description = "Detection counts, unread alerts and recent activity land here in Phase 2.",
            actionLabel = "Continue",
            onAction = {},
        )
    }
}
