package com.securevision.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.securevision.core.model.Severity
import com.securevision.core.ui.R
import com.securevision.core.ui.preview.PreviewContainer
import com.securevision.core.ui.preview.ThemePreviews
import com.securevision.core.ui.theme.SecureVisionDimens
import com.securevision.core.ui.theme.SecureVisionTheme

/**
 * Compact badge showing how urgent an alert is.
 *
 * Severity is encoded twice — as a colour and as a word — so it is still
 * readable to someone who cannot distinguish the colours.
 *
 * @param severity The severity to display.
 * @param modifier Modifier applied to the badge.
 */
@Composable
fun SVSeverityBadge(
    severity: Severity,
    modifier: Modifier = Modifier,
) {
    val palette = SecureVisionTheme.colors

    val container: Color
    val content: Color
    val dot: Color

    when (severity) {
        Severity.LOW -> {
            container = MaterialTheme.colorScheme.surfaceContainerHighest
            content = MaterialTheme.colorScheme.onSurfaceVariant
            dot = MaterialTheme.colorScheme.onSurfaceVariant
        }
        Severity.MEDIUM -> {
            container = palette.motionContainer
            content = palette.onMotionContainer
            dot = palette.motion
        }
        Severity.HIGH -> {
            container = palette.weaponContainer
            content = palette.onWeaponContainer
            dot = palette.weapon
        }
        Severity.CRITICAL -> {
            container = palette.unknownContainer
            content = palette.onUnknownContainer
            dot = palette.unknown
        }
    }

    Row(
        modifier = modifier
            .background(color = container, shape = MaterialTheme.shapes.extraSmall)
            .padding(
                horizontal = SecureVisionDimens.spacingSmall,
                vertical = SecureVisionDimens.spacingExtraSmall,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SecureVisionDimens.spacingExtraSmall),
    ) {
        Box(
            modifier = Modifier
                .size(SecureVisionDimens.spacingSmall)
                .background(color = dot, shape = CircleShape),
        )

        Text(
            text = stringResource(severity.labelRes()),
            style = MaterialTheme.typography.labelSmall,
            color = content,
        )
    }
}

/** Maps a severity onto its localised label. */
private fun Severity.labelRes(): Int = when (this) {
    Severity.LOW -> R.string.sv_severity_low
    Severity.MEDIUM -> R.string.sv_severity_medium
    Severity.HIGH -> R.string.sv_severity_high
    Severity.CRITICAL -> R.string.sv_severity_critical
}

@ThemePreviews
@Composable
private fun SVSeverityBadgePreview() {
    PreviewContainer {
        Row(horizontalArrangement = Arrangement.spacedBy(SecureVisionDimens.spacingSmall)) {
            Severity.entries.forEach { severity ->
                SVSeverityBadge(severity = severity)
            }
        }
    }
}
