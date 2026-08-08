package com.securevision.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import com.securevision.core.ui.preview.PreviewContainer
import com.securevision.core.ui.preview.ThemePreviews
import com.securevision.core.ui.theme.SecureVisionDimens
import com.securevision.core.ui.theme.SecureVisionTheme

/**
 * A single dashboard metric: an icon, a number, and what the number counts.
 *
 * The value and label are merged into one accessibility node reading
 * "12 alerts today", because a screen reader announcing "12" and "Alerts today"
 * as two separate stops conveys much less.
 *
 * @param icon Icon representing the metric.
 * @param value The metric, already formatted for display.
 * @param label What the metric counts.
 * @param modifier Modifier applied to the card.
 * @param accentColor Tint for the icon; defaults to the theme accent.
 * @param onClick Invoked on tap; `null` makes the card non-interactive.
 */
@Composable
fun SVStatCard(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    onClick: (() -> Unit)? = null,
) {
    SVCard(
        modifier = modifier.clearAndSetSemantics { contentDescription = "$value $label" },
        onClick = onClick,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SecureVisionDimens.spacingMediumSmall),
        ) {
            Box(
                modifier = Modifier
                    .background(
                        color = accentColor.copy(alpha = ICON_BACKGROUND_ALPHA),
                        shape = MaterialTheme.shapes.small,
                    )
                    .padding(SecureVisionDimens.spacingSmall),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(SecureVisionDimens.iconMedium),
                    tint = accentColor,
                )
            }

            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Keeps the icon plate readable against both the light and dark card surface. */
private const val ICON_BACKGROUND_ALPHA = 0.16f

@ThemePreviews
@Composable
private fun SVStatCardPreview() {
    PreviewContainer {
        SVStatCard(
            icon = Icons.Outlined.People,
            value = "12",
            label = "Enrolled profiles",
        )
        SVStatCard(
            icon = Icons.Outlined.Warning,
            value = "3",
            label = "Unread critical alerts",
            accentColor = SecureVisionTheme.colors.weapon,
        )
    }
}
