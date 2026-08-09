package com.securevision.core.domain.usecase.auth

import com.securevision.core.common.dispatcher.DispatcherProvider
import com.securevision.core.domain.repository.AuthRepository
import com.securevision.core.domain.usecase.UseCase
import com.securevision.core.model.UserAccount
import javax.inject.Inject

/**
 * Signs in the operator account.
 *
 * Only checks that the fields are present. Length and format rules deliberately
 * are not applied here: an existing account created under older rules must still
 * be able to sign in, and rejecting its password locally would lock the operator
 * out of their own device.
 */
class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    dispatcherProvider: DispatcherProvider,
) : UseCase<LoginUseCase.Params, UserAccount>(dispatcherProvider.io) {

    /**
     * @property username Handle chosen at sign-up.
     * @property password Plain-text password.
     */
    data class Params(
        val username: String,
        val password: String,
    )

    override suspend fun execute(parameters: Params): UserAccount {
        val username = parameters.username.trim()

        if (username.isEmpty()) {
            throw AuthValidationException(AuthValidationException.Reason.BLANK_USERNAME)
        }
        if (parameters.password.isEmpty()) {
            throw AuthValidationException(AuthValidationException.Reason.BLANK_PASSWORD)
        }

        return authRepository.login(username = username, password = parameters.password)
    }
}
