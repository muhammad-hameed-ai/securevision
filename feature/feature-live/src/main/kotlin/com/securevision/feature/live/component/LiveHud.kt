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
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.securevision.core.model.MotionResult
import com.securevision.core.ui.theme.SecureVisionDimens
import com.securevision.core.ui.theme.SecureVisionTheme
import com.securevision.feature.live.R

/**
 * The top overlay strip: the live indicator, the motion meter and the flip control.
 *
 * All three sit in one row with the meter *between* the other two. Previously the
 * meter was positioned independently against the same screen edge as the flip
 * button and overlapped it, swallowing taps meant for the camera switch. Laying
 * them out as siblings makes an overlap impossible rather than merely unlikely.
 *
 * @param isFrontCamera Which lens is active, for the control's description.
 * @param motion Latest frame comparison, shown as a meter.
 * @param isSwitchingCamera Whether a lens change is in flight.
 * @param onFlipCamera Switches lens.
 * @param modifier Modifier applied to the strip.
 */
@Composable
fun LiveHud(
    isFrontCamera: Boolean,
    motion: MotionResult,
    isSwitchingCamera: Boolean,
    hasTorch: Boolean,
    isTorchOn: Boolean,
    onToggleTorch: () -> Unit,
    onFlipCamera: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(SecureVisionTheme.colors.cameraScrim)
            .padding(
                start = SecureVisionDimens.spacingMedium,
                end = SecureVisionDimens.spacingSmall,
                top = SecureVisionDimens.spacingSmall,
                bottom = SecureVisionDimens.spacingSmall,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SecureVisionDimens.spacingMedium),
    ) {
        LiveIndicator()

        MotionIndicator(motion = motion, modifier = Modifier.weight(1f))

        // Only when the active camera has a torch. Most front cameras do not, and
        // showing a dead control is worse than showing none.
        if (hasTorch) {
            IconButton(
                onClick = onToggleTorch,
                modifier = Modifier.size(FLIP_TARGET_SIZE),
            ) {
                Icon(
                    imageVector = if (isTorchOn) {
                        Icons.Filled.FlashOn
                    } else {
                        Icons.Filled.FlashOff
                    },
                    contentDescription = stringResource(
                        if (isTorchOn) R.string.live_torch_off else R.string.live_torch_on,
                    ),
                    tint = if (isTorchOn) {
                        SecureVisionTheme.colors.motion
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
            }
        }

        IconButton(
            onClick = onFlipCamera,
            // Deliberately always enabled. Disabling it during the rebind was
            // meant to stop taps queueing, but it did the opposite: the flag that
            // drove `enabled` was cleared by an analysed frame, so a slow or
            // stalled analysis left the control dead and silently swallowing
            // presses. Re-entry is bounded in the ViewModel by a short debounce
            // instead, where a rejected tap can at least be logged.
            modifier = Modifier
                .size(FLIP_TARGET_SIZE)
                // Above the banner and anything else sharing this corner, so a
                // tap near the edge cannot be claimed by a neighbour.
                .zIndex(1f),
        ) {
            if (isSwitchingCamera) {
                // CameraX cannot swap lenses on a bound lifecycle — it has to
                // rebind — so the wait is real. Showing it beats a dead button.
                CircularProgressIndicator(
                    modifier = Modifier.size(SecureVisionDimens.iconSmall),
                    strokeWidth = SWITCH_STROKE,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            } else {
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

/** Accessibility minimum for a touch target. */
private val FLIP_TARGET_SIZE = 48.dp

private val SWITCH_STROKE = 2.dp
