package com.securevision.core.domain.usecase.auth

import com.securevision.core.common.dispatcher.DispatcherProvider
import com.securevision.core.domain.repository.AuthRepository
import com.securevision.core.domain.usecase.UseCase
import com.securevision.core.model.AccountCreation
import javax.inject.Inject

/**
 * Creates the single operator account.
 *
 * Enforces every credential rule in [AuthRules] here, in the domain layer, so
 * the same rules hold no matter which screen collects the form. Validation runs
 * before the repository is touched, which keeps an obviously invalid form from
 * paying for a ~500 ms BCrypt hash.
 */
class SignUpUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    dispatcherProvider: DispatcherProvider,
) : UseCase<SignUpUseCase.Params, AccountCreation>(dispatcherProvider.io) {

    /**
     * @property username Unique handle.
     * @property fullName Account holder's display name.
     * @property cnic National identity number, with or without separators.
     * @property password Plain-text password.
     * @property confirmPassword Repeat of [password]; must match.
     */
    data class Params(
        val username: String,
        val fullName: String,
        val cnic: String,
        val password: String,
        val confirmPassword: String,
    )

    override suspend fun execute(parameters: Params): AccountCreation {
        val username = parameters.username.trim()
        val fullName = parameters.fullName.trim()
        val cnic = AuthRules.normaliseCnic(parameters.cnic)

        validate(parameters, username, fullName, cnic)

        return authRepository.signUp(
            username = username,
            fullName = fullName,
            cnic = cnic,
            password = parameters.password,
        )
    }

    private fun validate(
        parameters: Params,
        username: String,
        fullName: String,
        cnic: String,
    ) {
        val reason = when {
            username.isEmpty() -> AuthValidationException.Reason.BLANK_USERNAME

            username.length < AuthRules.MIN_USERNAME_LENGTH ->
                AuthValidationException.Reason.USERNAME_TOO_SHORT

            fullName.isEmpty() -> AuthValidationException.Reason.BLANK_FULL_NAME

            cnic.length != AuthRules.CNIC_DIGIT_COUNT ->
                AuthValidationException.Reason.INVALID_CNIC

            parameters.password.isEmpty() -> AuthValidationException.Reason.BLANK_PASSWORD

            parameters.password.length < AuthRules.MIN_PASSWORD_LENGTH ->
                AuthValidationException.Reason.PASSWORD_TOO_SHORT

            parameters.password.toByteArray().size > AuthRules.MAX_PASSWORD_BYTES ->
                AuthValidationException.Reason.PASSWORD_TOO_LONG

            parameters.password != parameters.confirmPassword ->
                AuthValidationException.Reason.PASSWORD_CONFIRMATION_MISMATCH

            else -> null
        }

        if (reason != null) throw AuthValidationException(reason)
    }
}
