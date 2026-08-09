package com.securevision.feature.live.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.securevision.core.ui.theme.SecureVisionDimens
import com.securevision.core.ui.theme.SecureVisionTheme
import com.securevision.feature.live.R

/**
 * The control that stops a sounding critical alarm.
 *
 * Present only while the alarm is actually sounding. A permanently visible
 * Silence button invites pre-emptive tapping, and an alarm the operator has
 * already disarmed out of habit is no alarm at all.
 *
 * @param isSounding Whether a repeating alarm is playing.
 * @param onSilence Stops the tone. The alert stays recorded.
 * @param modifier Modifier applied to the button.
 */
@Composable
fun SilenceAlarmButton(
    isSounding: Boolean,
    onSilence: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = isSounding,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        // The pulse is the visual counterpart of a sound the user may not be able
        // to hear — a muted phone, a noisy room, or a hard-of-hearing operator.
        val transition = rememberInfiniteTransition(label = "alarm-pulse")
        val reduceMotion = rememberAnimationsDisabled()

        val pulse by transition.animateFloat(
            initialValue = 1f,
            targetValue = if (reduceMotion) 1f else PULSE_MIN_ALPHA,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = PULSE_MILLIS),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "alarm-pulse-alpha",
        )

        ExtendedFloatingActionButton(
            onClick = onSilence,
            containerColor = SecureVisionTheme.colors.weapon,
            contentColor = SecureVisionTheme.colors.onWeapon,
            modifier = Modifier.alpha(pulse),
        ) {
            Icon(imageVector = Icons.Outlined.NotificationsOff, contentDescription = null)
            Text(
                text = stringResource(R.string.live_silence_alarm),
                modifier = Modifier.padding(start = SecureVisionDimens.spacingSmall),
            )
        }
    }
}

/**
 * Whether the user has turned system animations off.
 *
 * Android exposes no "prefers reduced motion" flag; setting the animator duration
 * scale to zero in developer options or accessibility settings is how people
 * actually express it, so that is what this reads.
 */
@Composable
private fun rememberAnimationsDisabled(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            DEFAULT_ANIMATION_SCALE,
        ) == 0f
    }
}

private const val PULSE_MILLIS = 650
private const val PULSE_MIN_ALPHA = 0.55f
private const val DEFAULT_ANIMATION_SCALE = 1f
