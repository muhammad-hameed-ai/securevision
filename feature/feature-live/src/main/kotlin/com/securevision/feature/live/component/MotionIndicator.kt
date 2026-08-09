package com.securevision.feature.live.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.securevision.core.model.MotionResult
import com.securevision.core.ui.theme.SecureVisionDimens
import com.securevision.core.ui.theme.SecureVisionTheme
import com.securevision.feature.live.R

/**
 * A live meter showing how much of the frame is changing.
 *
 * A meter rather than a lamp: seeing the actual level is what makes the motion
 * threshold tunable, since the operator can watch what a still room reads versus
 * a person walking through it. A binary indicator would leave threshold tuning to
 * guesswork.
 *
 * @param motion Latest frame comparison.
 * @param modifier Modifier applied to the indicator.
 */
@Composable
fun MotionIndicator(
    motion: MotionResult,
    modifier: Modifier = Modifier,
) {
    val palette = SecureVisionTheme.colors

    val level by animateFloatAsState(
        // Scaled so ordinary movement fills a useful part of the bar; the raw
        // fraction rarely exceeds a few percent even for a person walking past.
        targetValue = (motion.intensity * METER_GAIN).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = SETTLE_MILLIS),
        label = "motion-level",
    )

    val colour by animateColorAsState(
        targetValue = if (motion.hasMotion) palette.motion else MaterialTheme.colorScheme.outline,
        animationSpec = tween(durationMillis = SETTLE_MILLIS),
        label = "motion-colour",
    )

    val description = stringResource(
        if (motion.hasMotion) R.string.live_motion_detected else R.string.live_motion_still,
    )

    Row(
        modifier = modifier
            .background(SecureVisionTheme.colors.cameraScrim)
            .padding(
                horizontal = SecureVisionDimens.spacingMediumSmall,
                vertical = SecureVisionDimens.spacingSmall,
            )
            .clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SecureVisionDimens.spacingSmall),
    ) {
        Box(
            modifier = Modifier
                .size(SecureVisionDimens.spacingSmall)
                .background(colour, CircleShape),
        )

        Text(
            text = stringResource(R.string.live_motion_label),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Box(
            modifier = Modifier
                .width(METER_WIDTH)
                .height(METER_HEIGHT)
                .background(
                    MaterialTheme.colorScheme.surfaceContainerHighest,
                    MaterialTheme.shapes.extraSmall,
                ),
        ) {
            // Width is computed rather than taken as a fraction: `fillMaxWidth`
            // rejects a zero fraction, and a still scene produces exactly that.
            Box(
                modifier = Modifier
                    .width(METER_WIDTH * level)
                    .height(METER_HEIGHT)
                    .background(colour, MaterialTheme.shapes.extraSmall),
            )
        }
    }
}

private val METER_WIDTH = 64.dp
private val METER_HEIGHT = 4.dp

/** Amplifies the raw changed-pixel fraction into a readable bar. */
private const val METER_GAIN = 8f

private const val SETTLE_MILLIS = 250
