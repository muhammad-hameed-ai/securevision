package com.securevision.core.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.securevision.core.ui.R
import com.securevision.core.ui.preview.PreviewContainer
import com.securevision.core.ui.preview.ThemePreviews
import com.securevision.core.ui.theme.SecureVisionDimens

/**
 * The primary call to action on a screen.
 *
 * While [loading] the button is disabled and shows a spinner in place of its
 * label, which is what stops a double tap submitting a form twice.
 *
 * @param text Button label.
 * @param onClick Invoked on tap. Not called while [loading] or when disabled.
 * @param modifier Modifier applied to the button.
 * @param enabled Whether the action is available.
 * @param loading Whether the action is currently running.
 */
@Composable
fun SVPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(SecureVisionDimens.buttonHeight),
        enabled = enabled && !loading,
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        AnimatedContent(
            targetState = loading,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "SVPrimaryButtonContent",
        ) { isLoading ->
            Box(contentAlignment = Alignment.Center) {
                if (isLoading) {
                    val loadingDescription =
                        stringResource(R.string.sv_content_description_loading)
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(SecureVisionDimens.buttonProgressSize)
                            .semantics { contentDescription = loadingDescription },
                        strokeWidth = SecureVisionDimens.buttonProgressStroke,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@ThemePreviews
@Composable
private fun SVPrimaryButtonPreview() {
    PreviewContainer {
        SVPrimaryButton(text = "Sign in", onClick = {})
        SVPrimaryButton(text = "Sign in", onClick = {}, loading = true)
        SVPrimaryButton(text = "Sign in", onClick = {}, enabled = false)
    }
}
