package com.securevision.core.domain.usecase.auth

import com.securevision.core.common.dispatcher.DispatcherProvider
import com.securevision.core.domain.repository.AuthRepository
import com.securevision.core.domain.usecase.UseCase
import com.securevision.core.model.UserAccount
import javax.inject.Inject

/**
 * Signs in an existing app-login account.
 *
 * Validates the input before touching the network, so an obviously empty form
 * fails instantly and offline rather than after a round trip.
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
