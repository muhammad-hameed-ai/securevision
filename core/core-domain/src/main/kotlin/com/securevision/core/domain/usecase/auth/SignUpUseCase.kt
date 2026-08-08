package com.securevision.core.domain.usecase.auth

import com.securevision.core.common.dispatcher.DispatcherProvider
import com.securevision.core.domain.repository.AuthRepository
import com.securevision.core.domain.usecase.UseCase
import com.securevision.core.model.UserAccount
import javax.inject.Inject

/**
 * Registers a new app-login account and signs it in.
 *
 * Enforces every credential rule in [AuthRules] here, in the domain layer, so
 * the same rules apply no matter which screen collects the form.
 */
class SignUpUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    dispatcherProvider: DispatcherProvider,
) : UseCase<SignUpUseCase.Params, UserAccount>(dispatcherProvider.io) {

    /**
     * @property username Unique handle.
     * @property fullName Account holder's display name.
     * @property password Plain-text password.
     * @property cnic National identity number, with or without separators.
     */
    data class Params(
        val username: String,
        val fullName: String,
        val password: String,
        val cnic: String,
    )

    override suspend fun execute(parameters: Params): UserAccount {
        val username = parameters.username.trim()
        val fullName = parameters.fullName.trim()
        val cnic = AuthRules.normaliseCnic(parameters.cnic)

        when {
            username.isEmpty() ->
                throw AuthValidationException(AuthValidationException.Reason.BLANK_USERNAME)

            username.length < AuthRules.MIN_USERNAME_LENGTH ->
                throw AuthValidationException(AuthValidationException.Reason.USERNAME_TOO_SHORT)

            fullName.isEmpty() ->
                throw AuthValidationException(AuthValidationException.Reason.BLANK_FULL_NAME)

            parameters.password.isEmpty() ->
                throw AuthValidationException(AuthValidationException.Reason.BLANK_PASSWORD)

            parameters.password.length < AuthRules.MIN_PASSWORD_LENGTH ->
                throw AuthValidationException(AuthValidationException.Reason.PASSWORD_TOO_SHORT)

            cnic.length != AuthRules.CNIC_DIGIT_COUNT ->
                throw AuthValidationException(AuthValidationException.Reason.INVALID_CNIC)
        }

        return authRepository.signUp(
            username = username,
            fullName = fullName,
            password = parameters.password,
            cnic = cnic,
        )
    }
}
