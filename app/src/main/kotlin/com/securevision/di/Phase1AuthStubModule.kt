package com.securevision.di

import com.securevision.core.domain.repository.AuthRepository
import com.securevision.core.model.AuthSession
import com.securevision.core.model.UserAccount
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Temporary Phase 1 binding: reports that nobody is signed in.
 *
 * The real Firebase-backed implementation lands in Phase 3 in `:core:core-data`.
 * Wiring the stub through Hilt rather than hard-coding `isLoggedIn = false` in a
 * Composable means the whole path — use case, `Result`, `StateFlow`, shell — is
 * exercised from the first commit, and switching to the real thing is a
 * one-line change to the `@Binds` below.
 *
 * DELETE THIS FILE IN PHASE 3.
 */
@Singleton
class SignedOutAuthRepository @Inject constructor() : AuthRepository {

    override suspend fun signUp(
        username: String,
        fullName: String,
        password: String,
        cnic: String,
    ): UserAccount = throw NotImplementedError(PHASE_3_MESSAGE)

    override suspend fun login(username: String, password: String): UserAccount =
        throw NotImplementedError(PHASE_3_MESSAGE)

    override suspend fun logout() = Unit

    override fun getCurrentSession(): Flow<AuthSession> = flowOf(AuthSession.SignedOut)

    override suspend fun isLoggedIn(): Boolean = false

    private companion object {
        const val PHASE_3_MESSAGE =
            "Authentication arrives in Phase 3 with the Firebase-backed AuthRepository."
    }
}

/** Binds the Phase 1 [SignedOutAuthRepository]. Replaced in Phase 3. */
@Module
@InstallIn(SingletonComponent::class)
abstract class Phase1AuthStubModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(implementation: SignedOutAuthRepository): AuthRepository
}
