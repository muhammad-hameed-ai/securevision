package com.securevision.core.domain.usecase.auth

import com.securevision.core.common.dispatcher.DispatcherProvider
import com.securevision.core.domain.repository.AuthRepository
import com.securevision.core.domain.usecase.FlowUseCase
import com.securevision.core.model.AuthSession
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Streams the authentication state of the app-login account.
 *
 * The app shell collects this to decide between the login screen and the
 * dashboard, which is what makes a sign-out take effect everywhere at once.
 */
class ObserveSessionUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    dispatcherProvider: DispatcherProvider,
) : FlowUseCase<Unit, AuthSession>(dispatcherProvider.io) {

    override fun execute(parameters: Unit): Flow<AuthSession> =
        authRepository.getCurrentSession()
}
