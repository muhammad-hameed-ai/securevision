package com.securevision.feature.alerts.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.securevision.core.model.AlertRecord
import com.securevision.core.model.AlertType
import com.securevision.core.ui.component.SVCard
import com.securevision.core.ui.component.SVSeverityBadge
import com.securevision.core.ui.theme.SecureVisionDimens
import com.securevision.core.ui.theme.SecureVisionTheme
import com.securevision.core.common.extension.toFormattedDateTime
import com.securevision.feature.alerts.R
import kotlin.math.roundToInt

/**
 * One alert in the gallery.
 *
 * A severity-coloured stripe runs down the left edge so the list can be scanned
 * for the one row that matters without reading any of them. Colour is never the
 * only signal — the severity badge spells it out too, because a stripe alone is
 * useless to a colour-blind operator and invisible in a printed incident report.
 *
 * @param alert The alert to show.
 * @param onClick Opens the detail view.
 * @param modifier Modifier applied to the card.
 */
@Composable
fun AlertCard(
    alert: AlertRecord,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = alert.accentColour()

    SVCard(
        modifier = modifier,
        onClick = onClick,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
    ) {
        // IntrinsicSize.Min so the severity stripe runs the full height of
        // whatever the content turns out to be, rather than a guessed constant
        // that clips on a long name or floats short on a compact row.
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(STRIPE_WIDTH)
                    .fillMaxHeight()
                    .background(accent),
            )

            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(SecureVisionDimens.spacingMedium),
                horizontalArrangement = Arrangement.spacedBy(SecureVisionDimens.spacingMedium),
            ) {
                Thumbnail(alert = alert, accent = accent)

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(SecureVisionDimens.spacingExtraSmall),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(
                            SecureVisionDimens.spacingSmall,
                        ),
                    ) {
                        Text(
                            text = alert.title(),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f, fill = false),
                        )

                        SVSeverityBadge(severity = alert.severity)

                        if (!alert.isRead) {
                            Box(
                                modifier = Modifier
                                    .size(UNREAD_DOT)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                            )
                        }
                    }

                    Text(
                        text = alert.subtitle(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    // Only for face alerts. Beard and mask on a weapon or motion
                    // event would be noise: no face was involved.
                    if (alert.type == AlertType.UNKNOWN_PERSON) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(
                                SecureVisionDimens.spacingExtraSmall,
                            ),
                        ) {
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
        }
    }
}

@Composable
private fun Thumbnail(alert: AlertRecord, accent: Color) {
    Box(
        modifier = Modifier
            .size(THUMBNAIL_SIZE)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        val uri = alert.snapshotUri

        if (uri == null) {
            // Motion alerts often have no snapshot, and a snapshot write can fail
            // without costing the alert. The type icon stands in.
            Icon(
                imageVector = alert.type.icon(),
                contentDescription = null,
                tint = accent,
            )
            return@Box
        }

        SubcomposeAsyncImage(
            model = uri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth(),
            error = {
                Icon(
                    imageVector = alert.type.icon(),
                    contentDescription = null,
                    tint = accent,
                )
            },
        )
    }
}

@Composable
private fun AlertRecord.subtitle(): String {
    // The shared formatter in core-common, not java.text.DateFormat. Three
    // screens each picked their own width before this, which is why one showed
    // "8:49 am" and another "10:06:48" for the same kind of value.
    val time = timestamp.toFormattedDateTime()

    val camera = stringResource(
        if (cameraFacing == FRONT_CAMERA) R.string.alerts_camera_front else R.string.alerts_camera_back,
    )

    return stringResource(
        R.string.alerts_card_subtitle,
        time,
        camera,
        (confidence * PERCENT).roundToInt(),
    )
}

@Composable
private fun AlertRecord.accentColour(): Color {
    val palette = SecureVisionTheme.colors

    return when (type) {
        AlertType.WEAPON -> palette.weapon
        AlertType.UNKNOWN_PERSON -> palette.unknown
        AlertType.MOTION -> palette.motion
        AlertType.KNOWN_PERSON -> palette.known
    }
}

/**
 * The headline for one alert.
 *
 * A recognised person is named — "Recognised: Ayesha" — because the identity is
 * the entire content of that entry; the category alone would say nothing. Every
 * other type is fully described by its category, and the subject appears in the
 * body instead.
 */
@Composable
internal fun AlertRecord.title(): String = when (type) {
    AlertType.KNOWN_PERSON -> stringResource(R.string.alerts_type_known, label)
    else -> stringResource(type.titleRes())
}

internal fun AlertType.titleRes(): Int = when (this) {
    AlertType.WEAPON -> R.string.alerts_type_weapon
    AlertType.UNKNOWN_PERSON -> R.string.alerts_type_unknown
    AlertType.MOTION -> R.string.alerts_type_motion
    AlertType.KNOWN_PERSON -> R.string.alerts_type_known
}

internal fun AlertType.icon(): ImageVector = when (this) {
    AlertType.WEAPON -> Icons.Outlined.WarningAmber
    AlertType.UNKNOWN_PERSON -> Icons.Outlined.PersonOutline
    AlertType.MOTION -> Icons.Outlined.DirectionsRun
    AlertType.KNOWN_PERSON -> Icons.Outlined.Verified
}

private val STRIPE_WIDTH = 4.dp
private val THUMBNAIL_SIZE = 64.dp
private val UNREAD_DOT = 8.dp
private const val PERCENT = 100f
private const val FRONT_CAMERA = "front"
