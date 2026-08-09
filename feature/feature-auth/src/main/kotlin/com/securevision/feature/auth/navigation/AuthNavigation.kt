package com.securevision.feature.auth.navigation

import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.securevision.feature.auth.AuthRoutes
import com.securevision.feature.auth.login.LoginScreen
import com.securevision.feature.auth.login.LoginViewModel
import com.securevision.feature.auth.profile.ProfileScreen
import com.securevision.feature.auth.profile.ProfileViewModel
import com.securevision.feature.auth.recovery.ForgotPasswordScreen
import com.securevision.feature.auth.recovery.ForgotPasswordViewModel
import com.securevision.feature.auth.signup.RecoveryCodeScreen
import com.securevision.feature.auth.signup.SignUpScreen
import com.securevision.feature.auth.signup.SignUpViewModel

/**
 * Registers the sign-in destination.
 *
 * @param onAuthenticated Invoked after a successful sign-in.
 * @param onForgotPassword Opens the recovery flow.
 */
fun NavGraphBuilder.loginScreen(
    onAuthenticated: () -> Unit,
    onForgotPassword: () -> Unit,
) {
    composable(route = AuthRoutes.LOGIN) {
        val viewModel: LoginViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val formState by viewModel.formState.collectAsStateWithLifecycle()

        LoginScreen(
            uiState = uiState,
            formState = formState,
            onUsernameChange = viewModel::onUsernameChange,
            onPasswordChange = viewModel::onPasswordChange,
            onSubmit = viewModel::submit,
            onForgotPassword = onForgotPassword,
            onAuthenticated = onAuthenticated,
        )
    }
}

/**
 * Registers account creation, including the one-time recovery code.
 *
 * Both are one destination. Once the account exists the ViewModel holds the
 * recovery code, and that alone swaps the form for the code screen — so the code
 * never passes through a navigation argument, and the operator cannot reach the
 * app without first dismissing it deliberately.
 *
 * @param onSignUpComplete Invoked after the recovery code has been acknowledged.
 */
fun NavGraphBuilder.signUpScreen(onSignUpComplete: () -> Unit) {
    composable(route = AuthRoutes.SIGN_UP) {
        val viewModel: SignUpViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val formState by viewModel.formState.collectAsStateWithLifecycle()
        val recoveryCode by viewModel.recoveryCode.collectAsStateWithLifecycle()

        val issuedCode = recoveryCode

        if (issuedCode != null) {
            RecoveryCodeScreen(
                recoveryCode = issuedCode,
                onAcknowledged = {
                    viewModel.onRecoveryCodeAcknowledged()
                    onSignUpComplete()
                },
            )
        } else {
            SignUpScreen(
                uiState = uiState,
                formState = formState,
                onUsernameChange = viewModel::onUsernameChange,
                onFullNameChange = viewModel::onFullNameChange,
                onCnicChange = viewModel::onCnicChange,
                onPasswordChange = viewModel::onPasswordChange,
                onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
                onSubmit = viewModel::submit,
            )
        }
    }
}

/**
 * Registers the password recovery destination.
 *
 * @param onPasswordReset Called once the password has been changed; the caller
 *   returns the operator to Login so they sign in with the new password.
 */
fun NavGraphBuilder.forgotPasswordScreen(onPasswordReset: () -> Unit) {
    composable(route = AuthRoutes.FORGOT_PASSWORD) {
        val viewModel: ForgotPasswordViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val formState by viewModel.formState.collectAsStateWithLifecycle()

        ForgotPasswordScreen(
            uiState = uiState,
            formState = formState,
            onUsernameChange = viewModel::onUsernameChange,
            onRecoveryCodeChange = viewModel::onRecoveryCodeChange,
            onNewPasswordChange = viewModel::onNewPasswordChange,
            onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
            onSubmit = viewModel::submit,
            onPasswordReset = onPasswordReset,
        )
    }
}

/** Registers the signed-in operator's account destination. */
fun NavGraphBuilder.profileScreen() {
    composable(route = AuthRoutes.PROFILE) {
        val viewModel: ProfileViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        ProfileScreen(uiState = uiState, onLogout = viewModel::logout)
    }
}
