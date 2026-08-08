package com.securevision.feature.dashboard.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.securevision.core.common.extension.toFormattedDateTime
import com.securevision.core.model.AlertRecord
import com.securevision.core.model.AlertType
import com.securevision.core.model.Severity
import com.securevision.core.ui.component.SVCard
import com.securevision.core.ui.component.SVSeverityBadge
import com.securevision.core.ui.preview.PreviewContainer
import com.securevision.core.ui.preview.ThemePreviews
import com.securevision.core.ui.theme.SecureVisionDimens
import com.securevision.feature.dashboard.R
import kotlin.math.roundToInt

/**
 * One alert in the dashboard's recent list.
 *
 * @param alert The alert to render.
 * @param onClick Invoked when the row is tapped.
 * @param modifier Modifier applied to the row.
 */
@Composable
fun RecentAlertRow(
    alert: AlertRecord,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SVCard(modifier = modifier, onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(SecureVisionDimens.spacingSmall),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(SecureVisionDimens.spacingExtraSmall),
            ) {
                Text(
                    text = stringResource(alert.type.labelRes()),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = alert.timestamp.toFormattedDateTime(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(
                        R.string.dashboard_alert_confidence,
                        (alert.confidence * PERCENT).roundToInt(),
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SVSeverityBadge(severity = alert.severity)
        }
    }
}

/** Maps an alert category onto its localised label. */
private fun AlertType.labelRes(): Int = when (this) {
    AlertType.KNOWN_PERSON -> R.string.dashboard_alert_type_known_person
    AlertType.UNKNOWN_PERSON -> R.string.dashboard_alert_type_unknown_person
    AlertType.WEAPON -> R.string.dashboard_alert_type_weapon
    AlertType.MOTION -> R.string.dashboard_alert_type_motion
}

private const val PERCENT = 100f

@ThemePreviews
@Composable
private fun RecentAlertRowPreview() {
    PreviewContainer {
        RecentAlertRow(
            alert = AlertRecord(
                id = "a1",
                type = AlertType.UNKNOWN_PERSON,
                severity = Severity.CRITICAL,
                confidence = 0.87f,
                cameraFacing = "front",
                snapshotUri = null,
                hasBeard = true,
                hasMask = false,
                timestamp = 1_754_000_000_000L,
                isRead = false,
            ),
            onClick = {},
        )
    }
}
