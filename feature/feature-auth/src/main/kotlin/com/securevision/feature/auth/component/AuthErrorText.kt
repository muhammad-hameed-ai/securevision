package com.securevision.feature.auth.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import com.securevision.core.ui.theme.SecureVisionDimens
import com.securevision.feature.auth.AuthUiState

/**
 * The failure banner shown above an auth form's action button.
 *
 * Marked as an assertive live region so a screen reader announces the failure
 * when it appears — without it, someone using TalkBack would tap "Sign in",
 * hear nothing, and have no idea why they were not signed in.
 *
 * @param state Current auth state; renders only for [AuthUiState.Error].
 * @param modifier Modifier applied to the banner.
 */
@Composable
fun AuthErrorText(
    state: AuthUiState,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(visible = state is AuthUiState.Error) {
        val messageRes = (state as? AuthUiState.Error)?.messageRes

        if (messageRes != null) {
            Text(
                text = stringResource(messageRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = modifier
                    .fillMaxWidth()
                    .semantics { liveRegion = LiveRegionMode.Assertive }
                    .background(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.medium,
                    )
                    .padding(SecureVisionDimens.spacingMediumSmall),
            )
        }
    }
}
