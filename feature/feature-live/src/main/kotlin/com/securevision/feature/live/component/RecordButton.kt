package com.securevision.feature.live.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.securevision.core.common.extension.toDurationLabel
import com.securevision.core.ui.theme.SecureVisionDimens
import com.securevision.core.ui.theme.SecureVisionTheme
import com.securevision.feature.live.R

/**
 * Starts and stops clip capture.
 *
 * While recording it shows a pulsing red dot and the elapsed time. The timer is
 * not decoration: clips go to internal storage on a device with no SD card, and
 * knowing a recording has been running for eleven minutes is what stops it
 * filling the phone unnoticed.
 *
 * @param isRecording Whether capture is running.
 * @param elapsedMillis How long the current clip has been running.
 * @param enabled Whether video capture bound on this device.
 * @param onToggle Starts or stops recording.
 * @param modifier Modifier applied to the button.
 */
@Composable
fun RecordButton(
    isRecording: Boolean,
    elapsedMillis: Long,
    enabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "record-pulse")

    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording) PULSE_MIN_ALPHA else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = PULSE_MILLIS),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "record-pulse-alpha",
    )

    FilledTonalButton(
        onClick = onToggle,
        enabled = enabled,
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SecureVisionDimens.spacingSmall),
        ) {
            Box(
                modifier = Modifier
                    .size(DOT_SIZE)
                    .alpha(if (isRecording) pulse else 1f)
                    .background(SecureVisionTheme.colors.unknown, CircleShape),
            )

            Text(
                text = if (isRecording) {
                    elapsedMillis.toDurationLabel()
                } else {
                    stringResource(R.string.live_record)
                },
                style = MaterialTheme.typography.labelLarge,
                // Tabular digits so the timer does not jitter as the seconds tick.
                fontFamily = if (isRecording) FontFamily.Monospace else null,
            )
        }
    }
}

private val DOT_SIZE = 10.dp
private const val PULSE_MILLIS = 700
private const val PULSE_MIN_ALPHA = 0.25f
