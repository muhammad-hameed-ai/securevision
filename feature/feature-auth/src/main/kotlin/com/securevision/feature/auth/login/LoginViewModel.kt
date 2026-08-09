package com.securevision.feature.auth.login

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securevision.core.common.result.Result
import com.securevision.core.domain.usecase.auth.LoginUseCase
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
 * Editable state of the login form.
 *
 * @property username Current username input.
 * @property password Current password input.
 */
@Immutable
data class LoginFormState(
    val username: String = "",
    val password: String = "",
) {
    /** Whether the action button should be enabled. */
    val canSubmit: Boolean get() = username.isNotBlank() && password.isNotEmpty()
}

/** Drives the login screen. */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
) : ViewModel() {

    private val _formState = MutableStateFlow(LoginFormState())

    /** Current form contents. */
    val formState: StateFlow<LoginFormState> = _formState.asStateFlow()

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)

    /** Current submission state. */
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    /** @param value New username input. */
    fun onUsernameChange(value: String) {
        _formState.update { it.copy(username = value) }
        clearError()
    }

    /** @param value New password input. */
    fun onPasswordChange(value: String) {
        _formState.update { it.copy(password = value) }
        clearError()
    }

    /**
     * Submits the form.
     *
     * Ignored while a submission is already running, so a second tap during the
     * BCrypt verify — which takes a few hundred milliseconds — cannot start a
     * duplicate sign-in.
     */
    fun submit() {
        if (_uiState.value is AuthUiState.Loading) return

        val form = _formState.value
        _uiState.value = AuthUiState.Loading

        viewModelScope.launch {
            val result = loginUseCase(
                LoginUseCase.Params(username = form.username, password = form.password),
            )

            _uiState.value = when (result) {
                is Result.Success -> AuthUiState.Success
                is Result.Error -> AuthUiState.Error(authErrorMessageRes(result.throwable))
                Result.Loading -> AuthUiState.Loading
            }
        }
    }

    /** Clears a previous failure once the user starts correcting the form. */
    private fun clearError() {
        if (_uiState.value is AuthUiState.Error) {
            _uiState.value = AuthUiState.Idle
        }
    }
}
