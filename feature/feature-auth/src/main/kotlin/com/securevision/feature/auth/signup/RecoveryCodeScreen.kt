package com.securevision.feature.auth.signup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.securevision.core.ui.component.SVCard
import com.securevision.core.ui.component.SVPrimaryButton
import com.securevision.core.ui.preview.PreviewContainer
import com.securevision.core.ui.preview.ThemePreviews
import com.securevision.core.ui.theme.SecureVisionDimens
import com.securevision.core.ui.theme.SecureVisionTheme
import com.securevision.feature.auth.R
import com.securevision.feature.auth.component.AuthScaffold
import kotlinx.coroutines.launch

/**
 * Shows the one-time recovery code immediately after the account is created.
 *
 * This is the only moment the code can ever be displayed — the account store
 * holds a BCrypt hash of it and nothing else — so the screen states the
 * consequence plainly rather than burying it, and has no way to skip past it
 * other than the explicit confirmation.
 *
 * @param recoveryCode The code to display, grouped for legibility.
 * @param onAcknowledged Called when the user confirms they have written it down.
 * @param modifier Modifier applied to the screen.
 */
@Composable
fun RecoveryCodeScreen(
    recoveryCode: String,
    onAcknowledged: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val clipboard = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val copiedMessage = stringResource(R.string.auth_recovery_copied)
    val codeContentDescription =
        stringResource(R.string.auth_recovery_content_description, recoveryCode.toSpelledOut())

    AuthScaffold(
        title = stringResource(R.string.auth_recovery_title),
        subtitle = stringResource(R.string.auth_recovery_subtitle),
        modifier = modifier,
    ) {
        SVCard {
            Text(
                text = recoveryCode,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = CODE_LETTER_SPACING,
                ),
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    // Read out character by character; a screen reader saying
                    // "seven kilopascal four" would be useless here.
                    .clearAndSetSemantics { contentDescription = codeContentDescription },
            )

            OutlinedButton(
                onClick = {
                    clipboard.setText(AnnotatedString(recoveryCode))
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            message = copiedMessage,
                            duration = SnackbarDuration.Short,
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.size(SecureVisionDimens.iconSmall),
                )
                Text(
                    text = stringResource(R.string.auth_recovery_copy),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(start = SecureVisionDimens.spacingSmall),
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = SecureVisionTheme.colors.weaponContainer,
                    shape = MaterialTheme.shapes.medium,
                )
                .padding(SecureVisionDimens.spacingMediumSmall),
            horizontalArrangement = Arrangement.spacedBy(SecureVisionDimens.spacingSmall),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = Icons.Outlined.WarningAmber,
                contentDescription = null,
                modifier = Modifier.size(SecureVisionDimens.iconMedium),
                tint = SecureVisionTheme.colors.onWeaponContainer,
            )
            Text(
                text = stringResource(R.string.auth_recovery_warning),
                style = MaterialTheme.typography.bodySmall,
                color = SecureVisionTheme.colors.onWeaponContainer,
            )
        }

        SVPrimaryButton(
            text = stringResource(R.string.auth_recovery_confirm),
            onClick = onAcknowledged,
        )

        SnackbarHost(hostState = snackbarHostState)
    }
}

/** Spaces the characters so TalkBack reads the code out one symbol at a time. */
private fun String.toSpelledOut(): String = filter { it != '-' }.toCharArray().joinToString(" ")

private val CODE_LETTER_SPACING = 4.sp

@ThemePreviews
@Composable
private fun RecoveryCodeScreenPreview() {
    PreviewContainer {
        RecoveryCodeScreen(recoveryCode = "7KP4-QW9M-2XHT", onAcknowledged = {})
    }
}
