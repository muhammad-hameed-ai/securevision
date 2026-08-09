package com.securevision.feature.auth.signup

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.securevision.core.ui.component.SVPrimaryButton
import com.securevision.core.ui.component.SVTextField
import com.securevision.core.ui.preview.PreviewContainer
import com.securevision.core.ui.preview.ThemePreviews
import com.securevision.feature.auth.AuthUiState
import com.securevision.feature.auth.R
import com.securevision.feature.auth.component.AuthErrorText
import com.securevision.feature.auth.component.AuthScaffold

/**
 * Account creation screen, rendered from state alone.
 *
 * @param uiState Current submission state.
 * @param formState Current form contents.
 * @param onUsernameChange Invoked on username edits.
 * @param onFullNameChange Invoked on full name edits.
 * @param onCnicChange Invoked on CNIC edits.
 * @param onPasswordChange Invoked on password edits.
 * @param onConfirmPasswordChange Invoked on confirmation edits.
 * @param onSubmit Invoked when the action button is pressed.
 * @param modifier Modifier applied to the screen.
 *
 * Has no success callback: once the account exists the caller swaps this form
 * for the recovery-code screen, driven by the code itself becoming available.
 */
@Composable
fun SignUpScreen(
    uiState: AuthUiState,
    formState: SignUpFormState,
    onUsernameChange: (String) -> Unit,
    onFullNameChange: (String) -> Unit,
    onCnicChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val enabled = uiState !is AuthUiState.Loading

    AuthScaffold(
        title = stringResource(R.string.auth_signup_title),
        subtitle = stringResource(R.string.auth_signup_subtitle),
        modifier = modifier,
    ) {
        SVTextField(
            value = formState.username,
            onValueChange = onUsernameChange,
            label = stringResource(R.string.auth_field_username),
            enabled = enabled,
        )

        SVTextField(
            value = formState.fullName,
            onValueChange = onFullNameChange,
            label = stringResource(R.string.auth_field_full_name),
            enabled = enabled,
        )

        SVTextField(
            value = formState.cnic,
            onValueChange = onCnicChange,
            label = stringResource(R.string.auth_field_cnic),
            enabled = enabled,
            // Digits with optional dashes, so a number pad would block the dashes.
            keyboardType = KeyboardType.Number,
        )

        SVTextField(
            value = formState.password,
            onValueChange = onPasswordChange,
            label = stringResource(R.string.auth_field_password),
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
            text = stringResource(R.string.auth_signup_action),
            onClick = onSubmit,
            enabled = formState.canSubmit,
            loading = uiState is AuthUiState.Loading,
        )
    }
}

@ThemePreviews
@Composable
private fun SignUpScreenPreview() {
    PreviewContainer {
        SignUpScreen(
            uiState = AuthUiState.Idle,
            formState = SignUpFormState(
                username = "hameed",
                fullName = "Muhammad Hameed",
                cnic = "42101-1234567-1",
                password = "correct-horse",
                confirmPassword = "correct-horse",
            ),
            onUsernameChange = {},
            onFullNameChange = {},
            onCnicChange = {},
            onPasswordChange = {},
            onConfirmPasswordChange = {},
            onSubmit = {},
        )
    }
}
