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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import com.securevision.core.ui.theme.SecureVisionDimens
import com.securevision.core.ui.theme.SecureVisionTheme
import com.securevision.feature.live.R

/**
 * The top overlay strip: a live indicator and the camera flip control.
 *
 * @param isFrontCamera Which lens is active, for the control's description.
 * @param onFlipCamera Switches lens.
 * @param modifier Modifier applied to the strip.
 */
@Composable
fun LiveHud(
    isFrontCamera: Boolean,
    onFlipCamera: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(SecureVisionTheme.colors.cameraScrim)
            .padding(
                horizontal = SecureVisionDimens.spacingMedium,
                vertical = SecureVisionDimens.spacingSmall,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        LiveIndicator()

        IconButton(onClick = onFlipCamera) {
            Icon(
                imageVector = Icons.Filled.Cameraswitch,
                contentDescription = stringResource(
                    if (isFrontCamera) {
                        R.string.live_switch_to_back_camera
                    } else {
                        R.string.live_switch_to_front_camera
                    },
                ),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/**
 * A blinking dot and the word LIVE.
 *
 * The blink is the one piece of motion on this screen. It answers a question the
 * operator will otherwise ask constantly — is this actually running, or has it
 * frozen on the last frame?
 */
@Composable
private fun LiveIndicator(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "live-indicator")
    val dotAlpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = BLINK_MILLIS),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "live-dot-alpha",
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SecureVisionDimens.spacingSmall),
    ) {
        Box(
            modifier = Modifier
                .size(SecureVisionDimens.spacingSmall)
                .alpha(dotAlpha)
                .background(SecureVisionTheme.colors.unknown, CircleShape),
        )
        Text(
            text = stringResource(R.string.live_indicator),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private const val BLINK_MILLIS = 700
