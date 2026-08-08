package com.securevision.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.securevision.core.ui.preview.PreviewContainer
import com.securevision.core.ui.preview.ThemePreviews
import com.securevision.core.ui.theme.SecureVisionDimens

/**
 * The standard SecureVision surface for grouped content.
 *
 * @param modifier Modifier applied to the card.
 * @param onClick Invoked when the card is tapped. Pass `null` for a card that is
 *   not interactive, which also keeps it out of the accessibility click order.
 * @param contentPadding Inner padding around [content].
 * @param content Card content, laid out in a column.
 */
@Composable
fun SVCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(SecureVisionDimens.spacingMedium),
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
    )
    val elevation = CardDefaults.cardElevation(defaultElevation = SecureVisionDimens.cardElevation)

    if (onClick == null) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = colors,
            elevation = elevation,
        ) {
            CardBody(contentPadding, content)
        }
    } else {
        Card(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = colors,
            elevation = elevation,
        ) {
            CardBody(contentPadding, content)
        }
    }
}

@Composable
private fun CardBody(
    contentPadding: PaddingValues,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier.padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(SecureVisionDimens.spacingSmall),
        content = content,
    )
}

@ThemePreviews
@Composable
private fun SVCardPreview() {
    PreviewContainer {
        SVCard {
            Text("Last detection", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Unknown person at the back door, 08 Aug 2026, 14:32:07",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SVCard(onClick = {}) {
            Text("Tap to review", style = MaterialTheme.typography.titleMedium)
        }
    }
}
