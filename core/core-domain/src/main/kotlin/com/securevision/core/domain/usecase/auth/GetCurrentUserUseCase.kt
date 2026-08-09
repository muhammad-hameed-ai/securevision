package com.securevision.core.domain.usecase.auth

import com.securevision.core.common.dispatcher.DispatcherProvider
import com.securevision.core.domain.repository.AuthRepository
import com.securevision.core.domain.usecase.FlowUseCase
import com.securevision.core.model.UserAccount
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Streams the signed-in account, or `null` when signed out.
 *
 * Used by the account screen. [UserAccount] carries no password material, so
 * nothing sensitive travels to the presentation layer here.
 */
class GetCurrentUserUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    dispatcherProvider: DispatcherProvider,
) : FlowUseCase<Unit, UserAccount?>(dispatcherProvider.io) {

    override fun execute(parameters: Unit): Flow<UserAccount?> = authRepository.getCurrentUser()
}
