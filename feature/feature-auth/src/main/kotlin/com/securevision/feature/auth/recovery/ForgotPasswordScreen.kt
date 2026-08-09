package com.securevision.feature.auth.recovery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import com.securevision.core.ui.component.SVPrimaryButton
import com.securevision.core.ui.component.SVTextField
import com.securevision.core.ui.preview.PreviewContainer
import com.securevision.core.ui.preview.ThemePreviews
import com.securevision.feature.auth.AuthUiState
import com.securevision.feature.auth.R
import com.securevision.feature.auth.component.AuthErrorText
import com.securevision.feature.auth.component.AuthScaffold

/**
 * Password recovery using the code issued at sign-up.
 *
 * @param uiState Current submission state.
 * @param formState Current form contents.
 * @param onUsernameChange Invoked on username edits.
 * @param onRecoveryCodeChange Invoked on recovery code edits.
 * @param onNewPasswordChange Invoked on password edits.
 * @param onConfirmPasswordChange Invoked on confirmation edits.
 * @param onSubmit Invoked when the action button is pressed.
 * @param onPasswordReset Called once the password has been changed.
 * @param modifier Modifier applied to the screen.
 */
@Composable
fun ForgotPasswordScreen(
    uiState: AuthUiState,
    formState: ForgotPasswordFormState,
    onUsernameChange: (String) -> Unit,
    onRecoveryCodeChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onPasswordReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) onPasswordReset()
    }

    val enabled = uiState !is AuthUiState.Loading

    AuthScaffold(
        title = stringResource(R.string.auth_forgot_title),
        subtitle = stringResource(R.string.auth_forgot_subtitle),
        modifier = modifier,
    ) {
        SVTextField(
            value = formState.username,
            onValueChange = onUsernameChange,
            label = stringResource(R.string.auth_field_username),
            enabled = enabled,
        )

        SVTextField(
            value = formState.recoveryCode,
            onValueChange = onRecoveryCodeChange,
            label = stringResource(R.string.auth_field_recovery_code),
            enabled = enabled,
        )

        SVTextField(
            value = formState.newPassword,
            onValueChange = onNewPasswordChange,
            label = stringResource(R.string.auth_field_new_password),
            isPassword = true,
            enabled = enabled,
        )

        SVTextField(
            value = formState.confirmPassword,
            onValueChange = onConfirmPasswordChange,
            label = stringResource(R.string.auth_field_confirm_password),
            isPassword = true,
            enabled = enabled,
            imeAction = ImeAction.Done,
        )

        AuthErrorText(state = uiState)

        SVPrimaryButton(
            text = stringResource(R.string.auth_forgot_action),
            onClick = onSubmit,
            enabled = formState.canSubmit,
            loading = uiState is AuthUiState.Loading,
        )
    }
}

@ThemePreviews
@Composable
private fun ForgotPasswordScreenPreview() {
    PreviewContainer {
        ForgotPasswordScreen(
            uiState = AuthUiState.Error(R.string.auth_error_invalid_recovery_code),
            formState = ForgotPasswordFormState(
                username = "hameed",
                recoveryCode = "7KP4-QW9M-2XHT",
                newPassword = "brand-new-secret",
                confirmPassword = "brand-new-secret",
            ),
            onUsernameChange = {},
            onRecoveryCodeChange = {},
            onNewPasswordChange = {},
            onConfirmPasswordChange = {},
            onSubmit = {},
            onPasswordReset = {},
        )
    }
}
