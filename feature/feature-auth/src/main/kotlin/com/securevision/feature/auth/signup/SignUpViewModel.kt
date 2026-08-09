package com.securevision.feature.auth.signup

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securevision.core.common.result.Result
import com.securevision.core.domain.usecase.auth.SignUpUseCase
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
 * Editable state of the sign-up form.
 *
 * @property username Chosen handle.
 * @property fullName Account holder's name.
 * @property cnic National identity number as typed, separators included.
 * @property password Chosen password.
 * @property confirmPassword Repeat of [password].
 */
@Immutable
data class SignUpFormState(
    val username: String = "",
    val fullName: String = "",
    val cnic: String = "",
    val password: String = "",
    val confirmPassword: String = "",
) {
    /**
     * Whether the action button is enabled.
     *
     * Only checks that every field has something in it. The real rules live in
     * the domain, and are reported as errors rather than by silently disabling
     * the button — a greyed-out button with no explanation is a dead end.
     */
    val canSubmit: Boolean
        get() = username.isNotBlank() &&
            fullName.isNotBlank() &&
            cnic.isNotBlank() &&
            password.isNotEmpty() &&
            confirmPassword.isNotEmpty()
}

/**
 * Drives the sign-up screen.
 *
 * On success it holds the one-time recovery code in [recoveryCode] so the next
 * screen can display it. This is the only place that value ever exists in
 * memory; the account store keeps only a hash of it.
 */
@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val signUpUseCase: SignUpUseCase,
) : ViewModel() {

    private val _formState = MutableStateFlow(SignUpFormState())

    /** Current form contents. */
    val formState: StateFlow<SignUpFormState> = _formState.asStateFlow()

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)

    /** Current submission state. */
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _recoveryCode = MutableStateFlow<String?>(null)

    /** The one-time recovery code, available only after a successful sign-up. */
    val recoveryCode: StateFlow<String?> = _recoveryCode.asStateFlow()

    /** @param value New username input. */
    fun onUsernameChange(value: String) = update { it.copy(username = value) }

    /** @param value New full name input. */
    fun onFullNameChange(value: String) = update { it.copy(fullName = value) }

    /** @param value New CNIC input. */
    fun onCnicChange(value: String) = update { it.copy(cnic = value) }

    /** @param value New password input. */
    fun onPasswordChange(value: String) = update { it.copy(password = value) }

    /** @param value New password confirmation input. */
    fun onConfirmPasswordChange(value: String) = update { it.copy(confirmPassword = value) }

    /**
     * Submits the form.
     *
     * Ignored while a submission is already running: BCrypt at cost 12 takes a
     * few hundred milliseconds, and a duplicate tap would otherwise attempt a
     * second account.
     */
    fun submit() {
        if (_uiState.value is AuthUiState.Loading) return

        val form = _formState.value
        _uiState.value = AuthUiState.Loading

        viewModelScope.launch {
            val result = signUpUseCase(
                SignUpUseCase.Params(
                    username = form.username,
                    fullName = form.fullName,
                    cnic = form.cnic,
                    password = form.password,
                    confirmPassword = form.confirmPassword,
                ),
            )

            _uiState.value = when (result) {
                is Result.Success -> {
                    _recoveryCode.value = result.data.recoveryCode
                    AuthUiState.Success
                }
                is Result.Error -> AuthUiState.Error(authErrorMessageRes(result.throwable))
                Result.Loading -> AuthUiState.Loading
            }
        }
    }

    /**
     * Drops the recovery code once the user has confirmed they wrote it down.
     *
     * Deliberate: holding it any longer than necessary keeps recoverable
     * plaintext in memory for no benefit.
     */
    fun onRecoveryCodeAcknowledged() {
        _recoveryCode.value = null
    }

    private fun update(transform: (SignUpFormState) -> SignUpFormState) {
        _formState.update(transform)

        if (_uiState.value is AuthUiState.Error) {
            _uiState.value = AuthUiState.Idle
        }
    }
}
