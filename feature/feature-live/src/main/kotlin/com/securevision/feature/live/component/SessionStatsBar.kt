package com.securevision.feature.live.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import com.securevision.core.ui.theme.SecureVisionDimens
import com.securevision.core.ui.theme.SecureVisionTheme
import com.securevision.feature.live.R
import com.securevision.feature.live.SessionStats

/**
 * The bottom strip: how many faces this session has seen, and how they resolved.
 *
 * @param stats Counts for the current session.
 * @param modifier Modifier applied to the bar.
 */
@Composable
fun SessionStatsBar(
    stats: SessionStats,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(SecureVisionTheme.colors.cameraScrim)
            .padding(
                horizontal = SecureVisionDimens.spacingMedium,
                vertical = SecureVisionDimens.spacingMediumSmall,
            ),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        StatColumn(
            value = stats.total,
            label = stringResource(R.string.live_stat_total),
            colour = MaterialTheme.colorScheme.onSurface,
        )
        StatColumn(
            value = stats.known,
            label = stringResource(R.string.live_stat_known),
            colour = SecureVisionTheme.colors.known,
        )
        StatColumn(
            value = stats.unknown,
            label = stringResource(R.string.live_stat_unknown),
            colour = SecureVisionTheme.colors.unknown,
        )
    }
}

@Composable
private fun StatColumn(
    value: Int,
    label: String,
    colour: Color,
) {
    Column(
        // Merged into one node so a screen reader says "3 unknown" rather than
        // stopping separately on the number and then the word.
        modifier = Modifier.clearAndSetSemantics { contentDescription = "$value $label" },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleLarge,
            color = colour,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
