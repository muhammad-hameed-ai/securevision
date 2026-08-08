package com.securevision.feature.dashboard.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.securevision.core.ui.component.SVCard
import com.securevision.core.ui.preview.PreviewContainer
import com.securevision.core.ui.preview.ThemePreviews
import com.securevision.core.ui.theme.SecureVisionDimens
import com.securevision.feature.dashboard.R

/**
 * Headline card stating whether monitoring is running.
 *
 * @param modifier Modifier applied to the card.
 */
@Composable
fun SystemStatusCard(modifier: Modifier = Modifier) {
    SVCard(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SecureVisionDimens.spacingMedium),
        ) {
            Box(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = SHIELD_PLATE_ALPHA),
                        shape = CircleShape,
                    )
                    .padding(SecureVisionDimens.spacingMediumSmall),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Shield,
                    // Decorative: the title beside it states the status in words.
                    contentDescription = null,
                    modifier = Modifier.size(SecureVisionDimens.iconMedium),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(SecureVisionDimens.spacingExtraSmall)) {
                Text(
                    text = stringResource(R.string.dashboard_status_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.dashboard_status_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Keeps the shield plate readable on both the light and dark card surface. */
private const val SHIELD_PLATE_ALPHA = 0.16f

@ThemePreviews
@Composable
private fun SystemStatusCardPreview() {
    PreviewContainer {
        SystemStatusCard()
    }
}
