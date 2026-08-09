package com.securevision.core.domain.usecase.auth

import com.securevision.core.common.dispatcher.DispatcherProvider
import com.securevision.core.domain.repository.AuthRepository
import com.securevision.core.domain.usecase.UseCase
import com.securevision.core.model.UserAccount
import javax.inject.Inject

/**
 * Sets a new password using the recovery code issued at sign-up.
 *
 * The only way back into an account whose password has been forgotten. Without
 * it the sole remaining option would be clearing app data, which also destroys
 * every enrolled person profile.
 *
 * Succeeding does not sign the user in — they then log in with the new password,
 * which confirms it was set to what they intended.
 */
class ResetPasswordUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    dispatcherProvider: DispatcherProvider,
) : UseCase<ResetPasswordUseCase.Params, UserAccount>(dispatcherProvider.io) {

    /**
     * @property username Handle of the account to recover.
     * @property recoveryCode The code shown once at sign-up; separators and case
     *   are normalised before comparison.
     * @property newPassword Replacement password.
     * @property confirmPassword Repeat of [newPassword]; must match.
     */
    data class Params(
        val username: String,
        val recoveryCode: String,
        val newPassword: String,
        val confirmPassword: String,
    )

    override suspend fun execute(parameters: Params): UserAccount {
        val username = parameters.username.trim()
        val recoveryCode = AuthRules.normaliseRecoveryCode(parameters.recoveryCode)

        val reason = when {
            username.isEmpty() -> AuthValidationException.Reason.BLANK_USERNAME

            recoveryCode.length != AuthRules.RECOVERY_CODE_LENGTH ->
                AuthValidationException.Reason.INVALID_RECOVERY_CODE

            parameters.newPassword.isEmpty() -> AuthValidationException.Reason.BLANK_PASSWORD

            parameters.newPassword.length < AuthRules.MIN_PASSWORD_LENGTH ->
                AuthValidationException.Reason.PASSWORD_TOO_SHORT

            parameters.newPassword.toByteArray().size > AuthRules.MAX_PASSWORD_BYTES ->
                AuthValidationException.Reason.PASSWORD_TOO_LONG

            parameters.newPassword != parameters.confirmPassword ->
                AuthValidationException.Reason.PASSWORD_CONFIRMATION_MISMATCH

            else -> null
        }

        if (reason != null) throw AuthValidationException(reason)

        return authRepository.resetPassword(
            username = username,
            recoveryCode = recoveryCode,
            newPassword = parameters.newPassword,
        )
    }
}
