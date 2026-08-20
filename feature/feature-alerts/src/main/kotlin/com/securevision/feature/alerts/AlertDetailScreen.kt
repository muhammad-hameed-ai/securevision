package com.securevision.feature.alerts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ImageNotSupported
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import coil.compose.SubcomposeAsyncImage
import com.securevision.core.model.AlertRecord
import com.securevision.core.model.AlertType
import com.securevision.core.ui.component.SVEmptyState
import com.securevision.core.ui.component.SVSeverityBadge
import com.securevision.core.ui.component.SVTopBar
import com.securevision.core.ui.theme.SecureVisionDimens
import com.securevision.feature.alerts.component.AgeChip
import com.securevision.feature.alerts.component.AttributeChip
import com.securevision.core.common.extension.toFormattedDateTime
import com.securevision.feature.alerts.component.title
import kotlin.math.roundToInt

/**
 * One alert in full.
 *
 * @param alert The alert, or `null` if it no longer exists.
 * @param isLoading Whether the lookup is still running.
 * @param onBack Returns to the gallery.
 * @param modifier Modifier applied to the screen.
 */
@Composable
fun AlertDetailScreen(
    alert: AlertRecord?,
    isLoading: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            SVTopBar(title = stringResource(R.string.alerts_detail_title), onBack = onBack)
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                isLoading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary,
                )

                // Reachable in normal use: the list is a Flow, so an alert can be
                // dismissed on the gallery while its detail sits on the back stack.
                alert == null -> SVEmptyState(
                    icon = Icons.Outlined.ImageNotSupported,
                    title = stringResource(R.string.alerts_detail_missing),
                    subtitle = "",
                    modifier = Modifier.fillMaxSize(),
                )

                else -> AlertDetailContent(alert = alert)
            }
        }
    }
}

@Composable
private fun AlertDetailContent(alert: AlertRecord) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(SecureVisionDimens.spacingMedium),
        verticalArrangement = Arrangement.spacedBy(SecureVisionDimens.spacingMedium),
    ) {
        Snapshot(uri = alert.snapshotUri)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SecureVisionDimens.spacingSmall),
        ) {
            Text(
                text = alert.title(),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
            )
            SVSeverityBadge(severity = alert.severity)
        }

        DetailRow(
            label = stringResource(R.string.alerts_detail_time),
            value = alert.timestamp.toFormattedDateTime(),
        )

        DetailRow(
            label = stringResource(R.string.alerts_detail_camera),
            value = stringResource(
                if (alert.cameraFacing == FRONT_CAMERA) {
                    R.string.alerts_camera_front
                } else {
                    R.string.alerts_camera_back
                },
            ),
        )

        DetailRow(
            label = stringResource(R.string.alerts_detail_confidence),
            value = "${(alert.confidence * PERCENT).roundToInt()}%",
        )

        if (alert.type == AlertType.UNKNOWN_PERSON) {
            Text(
                text = stringResource(R.string.alerts_detail_attributes),
                style = MaterialTheme.typography.labelLarge,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(SecureVisionDimens.spacingSmall)) {
                AttributeChip(
                    label = stringResource(R.string.alerts_attribute_beard),
                    value = alert.hasBeard,
                )
                AttributeChip(
                    label = stringResource(R.string.alerts_attribute_mask),
                    value = alert.hasMask,
                )
                AgeChip(estimatedAge = alert.estimatedAge)
            }
        }
    }
}

@Composable
private fun Snapshot(uri: String?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(SNAPSHOT_ASPECT)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        if (uri == null) {
            Text(
                text = stringResource(R.string.alerts_detail_no_snapshot),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Box
        }

        SubcomposeAsyncImage(
            model = uri,
            contentDescription = stringResource(R.string.alerts_detail_snapshot),
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
            error = {
                Icon(
                    imageVector = Icons.Outlined.ImageNotSupported,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

private const val SNAPSHOT_ASPECT = 4f / 3f
private const val PERCENT = 100f
private const val FRONT_CAMERA = "front"
