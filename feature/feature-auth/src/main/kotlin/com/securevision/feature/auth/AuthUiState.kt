package com.securevision.feature.auth

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.securevision.core.domain.usecase.auth.AuthValidationException

/**
 * The outcome of a credential submission, shared by every auth screen.
 *
 * One type across Login, Sign-up and password recovery so the three screens
 * behave identically: the same button spinner, the same error placement, the
 * same success handoff.
 */
@Immutable
sealed interface AuthUiState {

    /** Nothing submitted yet, or the last error has been dismissed. */
    data object Idle : AuthUiState

    /**
     * A submission is in flight.
     *
     * Not incidental: BCrypt at cost 12 takes a few hundred milliseconds, so this
     * state is visible on every real sign-in and is what stops a double tap
     * submitting twice.
     */
    data object Loading : AuthUiState

    /** The operation succeeded; the screen hands off to navigation. */
    data object Success : AuthUiState

    /**
     * The operation failed.
     *
     * @property messageRes Localised explanation, resolved from the domain reason.
     */
    data class Error(@param:StringRes val messageRes: Int) : AuthUiState
}

/**
 * Maps a domain failure onto the string the user actually reads.
 *
 * Lives in the presentation layer because that is the only layer with access to
 * `strings.xml`; the domain deliberately reports a reason, never a sentence.
 *
 * @param throwable The failure carried by `Result.Error`.
 */
@StringRes
fun authErrorMessageRes(throwable: Throwable?): Int {
    val reason = (throwable as? AuthValidationException)?.reason
        ?: return R.string.auth_error_unexpected

    return when (reason) {
        AuthValidationException.Reason.BLANK_USERNAME -> R.string.auth_error_blank_username
        AuthValidationException.Reason.USERNAME_TOO_SHORT -> R.string.auth_error_username_too_short
        AuthValidationException.Reason.BLANK_FULL_NAME -> R.string.auth_error_blank_full_name
        AuthValidationException.Reason.BLANK_PASSWORD -> R.string.auth_error_blank_password
        AuthValidationException.Reason.PASSWORD_TOO_SHORT -> R.string.auth_error_password_too_short
        AuthValidationException.Reason.PASSWORD_TOO_LONG -> R.string.auth_error_password_too_long
        AuthValidationException.Reason.PASSWORD_CONFIRMATION_MISMATCH ->
            R.string.auth_error_password_mismatch
        AuthValidationException.Reason.INVALID_CNIC -> R.string.auth_error_invalid_cnic
        AuthValidationException.Reason.ACCOUNT_ALREADY_EXISTS ->
            R.string.auth_error_account_exists
        AuthValidationException.Reason.USERNAME_TAKEN -> R.string.auth_error_username_taken
        AuthValidationException.Reason.INVALID_CREDENTIALS ->
            R.string.auth_error_invalid_credentials
        AuthValidationException.Reason.NO_ACCOUNT_EXISTS -> R.string.auth_error_no_account
        AuthValidationException.Reason.INVALID_RECOVERY_CODE ->
            R.string.auth_error_invalid_recovery_code
    }
}
