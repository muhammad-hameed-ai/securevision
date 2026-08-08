package com.securevision.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import com.securevision.core.ui.preview.PreviewContainer
import com.securevision.core.ui.preview.ThemePreviews
import com.securevision.core.ui.theme.SecureVisionDimens

/**
 * Placeholder shown when a list has nothing in it.
 *
 * @param icon Illustrative icon.
 * @param title Short statement of what is missing.
 * @param subtitle Sentence explaining how the list gets populated.
 * @param modifier Modifier applied to the layout.
 * @param action Optional call to action rendered beneath the text.
 */
@Composable
fun SVEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = SecureVisionDimens.spacingLarge,
                vertical = SecureVisionDimens.spacingHuge,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SecureVisionDimens.spacingMediumSmall),
    ) {
        Icon(
            imageVector = icon,
            // Decorative: the title beneath already carries the meaning.
            contentDescription = null,
            modifier = Modifier.size(SecureVisionDimens.iconLarge),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        action?.invoke()
    }
}

@ThemePreviews
@Composable
private fun SVEmptyStatePreview() {
    PreviewContainer {
        SVEmptyState(
            icon = Icons.Outlined.NotificationsOff,
            title = "No alerts yet",
            subtitle = "Alerts appear here as soon as the live view detects a face, " +
                "a weapon or movement.",
        )
    }
}
