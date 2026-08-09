package com.securevision.feature.auth.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.securevision.core.ui.component.SVPrimaryButton
import com.securevision.core.ui.component.SVTextField
import com.securevision.core.ui.preview.PreviewContainer
import com.securevision.core.ui.preview.ThemePreviews
import com.securevision.core.ui.theme.SecureVisionDimens
import com.securevision.feature.auth.AuthUiState
import com.securevision.feature.auth.R
import com.securevision.feature.auth.component.AuthErrorText
import com.securevision.feature.auth.component.AuthScaffold

/**
 * Sign-in screen, rendered from state alone.
 *
 * @param uiState Current submission state.
 * @param formState Current form contents.
 * @param onUsernameChange Invoked on username edits.
 * @param onPasswordChange Invoked on password edits.
 * @param onSubmit Invoked when the action button is pressed.
 * @param onForgotPassword Opens the recovery-code flow.
 * @param onAuthenticated Called once when sign-in succeeds.
 * @param modifier Modifier applied to the screen.
 */
@Composable
fun LoginScreen(
    uiState: AuthUiState,
    formState: LoginFormState,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onForgotPassword: () -> Unit,
    onAuthenticated: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Keyed on the state so navigation fires exactly once per success, not on
    // every recomposition that happens to observe it.
    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) onAuthenticated()
    }

    AuthScaffold(
        title = stringResource(R.string.auth_login_title),
        subtitle = stringResource(R.string.auth_login_subtitle),
        modifier = modifier,
    ) {
        SVTextField(
            value = formState.username,
            onValueChange = onUsernameChange,
            label = stringResource(R.string.auth_field_username),
            enabled = uiState !is AuthUiState.Loading,
            imeAction = ImeAction.Next,
        )

        SVTextField(
            value = formState.password,
            onValueChange = onPasswordChange,
            label = stringResource(R.string.auth_field_password),
            isPassword = true,
            enabled = uiState !is AuthUiState.Loading,
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
        )

        AuthErrorText(state = uiState)

        SVPrimaryButton(
            text = stringResource(R.string.auth_login_action),
            onClick = onSubmit,
            enabled = formState.canSubmit,
            loading = uiState is AuthUiState.Loading,
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(SecureVisionDimens.spacingExtraSmall),
        ) {
            TextButton(
                onClick = onForgotPassword,
                enabled = uiState !is AuthUiState.Loading,
            ) {
                Text(
                    text = stringResource(R.string.auth_login_forgot_password),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@ThemePreviews
@Composable
private fun LoginScreenPreview() {
    PreviewContainer {
        LoginScreen(
            uiState = AuthUiState.Idle,
            formState = LoginFormState(username = "hameed", password = "secret123"),
            onUsernameChange = {},
            onPasswordChange = {},
            onSubmit = {},
            onForgotPassword = {},
            onAuthenticated = {},
        )
    }
}

@ThemePreviews
@Composable
private fun LoginScreenErrorPreview() {
    PreviewContainer {
        LoginScreen(
            uiState = AuthUiState.Error(R.string.auth_error_invalid_credentials),
            formState = LoginFormState(username = "hameed", password = "wrong"),
            onUsernameChange = {},
            onPasswordChange = {},
            onSubmit = {},
            onForgotPassword = {},
            onAuthenticated = {},
        )
    }
}
