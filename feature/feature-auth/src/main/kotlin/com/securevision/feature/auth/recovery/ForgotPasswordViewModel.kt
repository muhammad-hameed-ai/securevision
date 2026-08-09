package com.securevision.feature.auth.recovery

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securevision.core.common.result.Result
import com.securevision.core.domain.usecase.auth.ResetPasswordUseCase
import com.securevision.feature.auth.AuthUiState
import com.securevision.feature.auth.authErrorMessageRes
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Editable state of the password recovery form.
 *
 * @property username Account to recover.
 * @property recoveryCode Code as typed; separators and case are normalised later.
 * @property newPassword Replacement password.
 * @property confirmPassword Repeat of [newPassword].
 */
@Immutable
data class ForgotPasswordFormState(
    val username: String = "",
    val recoveryCode: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
) {
    /** Whether the action button is enabled. */
    val canSubmit: Boolean
        get() = username.isNotBlank() &&
            recoveryCode.isNotBlank() &&
            newPassword.isNotEmpty() &&
            confirmPassword.isNotEmpty()
}

/**
 * Drives the password recovery screen.
 *
 * Success does not sign the operator in — they return to Login and use the new
 * password, which confirms it was set to what they intended rather than to a
 * typo they would discover with no recovery code left.
 */
@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val resetPasswordUseCase: ResetPasswordUseCase,
) : ViewModel() {

    private val _formState = MutableStateFlow(ForgotPasswordFormState())

    /** Current form contents. */
    val formState: StateFlow<ForgotPasswordFormState> = _formState.asStateFlow()

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)

    /** Current submission state. */
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    /** @param value New username input. */
    fun onUsernameChange(value: String) = update { it.copy(username = value) }

    /** @param value New recovery code input. */
    fun onRecoveryCodeChange(value: String) = update { it.copy(recoveryCode = value) }

    /** @param value New password input. */
    fun onNewPasswordChange(value: String) = update { it.copy(newPassword = value) }

    /** @param value New password confirmation input. */
    fun onConfirmPasswordChange(value: String) = update { it.copy(confirmPassword = value) }

    /** Submits the form. Ignored while a submission is already running. */
    fun submit() {
        if (_uiState.value is AuthUiState.Loading) return

        val form = _formState.value
        _uiState.value = AuthUiState.Loading

        viewModelScope.launch {
            val result = resetPasswordUseCase(
                ResetPasswordUseCase.Params(
                    username = form.username,
                    recoveryCode = form.recoveryCode,
                    newPassword = form.newPassword,
                    confirmPassword = form.confirmPassword,
                ),
            )

            _uiState.value = when (result) {
                is Result.Success -> AuthUiState.Success
                is Result.Error -> AuthUiState.Error(authErrorMessageRes(result.throwable))
                Result.Loading -> AuthUiState.Loading
            }
        }
    }

    private fun update(transform: (ForgotPasswordFormState) -> ForgotPasswordFormState) {
        _formState.update(transform)

        if (_uiState.value is AuthUiState.Error) {
            _uiState.value = AuthUiState.Idle
        }
    }
}
