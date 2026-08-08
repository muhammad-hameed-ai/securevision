package com.securevision.core.domain.usecase.auth

import com.securevision.core.common.dispatcher.DispatcherProvider
import com.securevision.core.domain.repository.AuthRepository
import com.securevision.core.domain.usecase.UseCase
import javax.inject.Inject

/** Signs the current account out and clears the persisted session. */
class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    dispatcherProvider: DispatcherProvider,
) : UseCase<Unit, Unit>(dispatcherProvider.io) {

    override suspend fun execute(parameters: Unit) = authRepository.logout()
}
