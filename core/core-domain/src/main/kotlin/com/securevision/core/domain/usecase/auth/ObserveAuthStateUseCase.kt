package com.securevision.core.domain.usecase.auth

import com.securevision.core.common.dispatcher.DispatcherProvider
import com.securevision.core.domain.repository.AuthRepository
import com.securevision.core.domain.usecase.FlowUseCase
import com.securevision.core.model.AuthSession
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Streams the launch gate: whether an account exists, and whether it is signed in.
 *
 * The app shell collects this to choose between sign-up, login and the
 * dashboard, which is what makes a logout take effect everywhere at once.
 */
class ObserveAuthStateUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    dispatcherProvider: DispatcherProvider,
) : FlowUseCase<Unit, AuthSession>(dispatcherProvider.io) {

    override fun execute(parameters: Unit): Flow<AuthSession> = authRepository.observeAuthState()
}
