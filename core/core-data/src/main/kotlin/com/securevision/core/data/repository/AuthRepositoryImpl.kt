package com.securevision.core.data.repository

import com.securevision.core.common.dispatcher.DispatcherProvider
import com.securevision.core.data.database.dao.UserAccountDao
import com.securevision.core.data.database.entity.UserAccountEntity
import com.securevision.core.data.mapper.toDomain
import com.securevision.core.data.security.PasswordHasher
import com.securevision.core.data.security.RecoveryCodeGenerator
import com.securevision.core.data.session.SessionManager
import com.securevision.core.domain.repository.AuthRepository
import com.securevision.core.domain.usecase.auth.AuthRules
import com.securevision.core.domain.usecase.auth.AuthValidationException
import com.securevision.core.model.AccountCreation
import com.securevision.core.model.AuthSession
import com.securevision.core.model.UserAccount
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext

/**
 * On-device app-login account, backed by Room and a persisted session.
 *
 * No network. The password and recovery code exist as plaintext only for the
 * duration of a call; what reaches storage is a BCrypt hash of each.
 *
 * @property dao Account table access.
 * @property passwordHasher Hashes and verifies both secrets.
 * @property recoveryCodeGenerator Issues the one-time recovery code.
 * @property sessionManager Persists which account is signed in.
 * @property dispatcherProvider Supplies the IO dispatcher; BCrypt at cost 12
 *   takes a few hundred milliseconds and must never run on the main thread.
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val dao: UserAccountDao,
    private val passwordHasher: PasswordHasher,
    private val recoveryCodeGenerator: RecoveryCodeGenerator,
    private val sessionManager: SessionManager,
    private val dispatcherProvider: DispatcherProvider,
) : AuthRepository {

    override suspend fun signUp(
        username: String,
        fullName: String,
        cnic: String,
        password: String,
    ): AccountCreation = withContext(dispatcherProvider.io) {
        if (dao.count() > 0) {
            throw AuthValidationException(AuthValidationException.Reason.ACCOUNT_ALREADY_EXISTS)
        }
        if (dao.getByUsername(username) != null) {
            throw AuthValidationException(AuthValidationException.Reason.USERNAME_TAKEN)
        }

        val recoveryCode = recoveryCodeGenerator.generate()

        val account = UserAccountEntity(
            uid = UUID.randomUUID().toString(),
            username = username,
            fullName = fullName,
            cnic = cnic,
            passwordHash = passwordHasher.hash(password),
            // Hash the normalised form so the code can later be typed with or
            // without its dashes, in any case.
            recoveryCodeHash = passwordHasher.hash(
                AuthRules.normaliseRecoveryCode(recoveryCode),
            ),
            createdAt = System.currentTimeMillis(),
        )

        dao.insert(account)
        sessionManager.setSession(account.uid)

        AccountCreation(account = account.toDomain(), recoveryCode = recoveryCode)
    }

    override suspend fun login(username: String, password: String): UserAccount =
        withContext(dispatcherProvider.io) {
            // Same reason for an unknown username and a wrong password, so the
            // error cannot be used to discover which usernames exist.
            //
            // No dummy hash is performed for the unknown-username path: the
            // timing difference only matters to an attacker who already holds the
            // device, and such an attacker can read the hash out of the database
            // directly. Defending the timing channel here would buy nothing.
            val account = dao.getByUsername(username)
                ?: throw AuthValidationException(
                    AuthValidationException.Reason.INVALID_CREDENTIALS,
                )

            if (!passwordHasher.verify(password, account.passwordHash)) {
                throw AuthValidationException(
                    AuthValidationException.Reason.INVALID_CREDENTIALS,
                )
            }

            sessionManager.setSession(account.uid)

            account.toDomain()
        }

    override suspend fun logout() {
        sessionManager.clearSession()
    }

    /**
     * Deliberately does not sign the user in on success.
     *
     * Making them log in with the new password immediately afterwards confirms it
     * was set to what they intended, rather than to a typo they will discover on
     * the next launch with no recovery code left to use.
     */
    override suspend fun resetPassword(
        username: String,
        recoveryCode: String,
        newPassword: String,
    ): UserAccount = withContext(dispatcherProvider.io) {
        val account = dao.getByUsername(username)
            ?: throw AuthValidationException(
                AuthValidationException.Reason.INVALID_RECOVERY_CODE,
            )

        if (!passwordHasher.verify(recoveryCode, account.recoveryCodeHash)) {
            throw AuthValidationException(
                AuthValidationException.Reason.INVALID_RECOVERY_CODE,
            )
        }

        val updated = account.copy(passwordHash = passwordHasher.hash(newPassword))
        dao.insert(updated)

        updated.toDomain()
    }

    /**
     * Combines "does an account exist" with "is a session active" into the single
     * value the launch gate needs.
     *
     * Opens with [AuthSession.Unknown] so the shell shows a splash rather than
     * flashing the login screen for one frame before a valid session resolves.
     */
    override fun observeAuthState(): Flow<AuthSession> =
        combine(dao.observeAccount(), sessionManager.currentSession) { account, sessionUid ->
            when {
                account == null -> AuthSession.NoAccount
                sessionUid == null -> AuthSession.SignedOut
                sessionUid == account.uid -> AuthSession.SignedIn(account.toDomain())
                // A session pointing at an account that no longer exists. Treated
                // as signed out rather than trusted, so a stale uid can never
                // authenticate against a different account.
                else -> AuthSession.SignedOut
            }
        }.onStart { emit(AuthSession.Unknown) }

    override fun getCurrentUser(): Flow<UserAccount?> = observeAuthState()
        .map { session -> (session as? AuthSession.SignedIn)?.account }

    override fun isLoggedIn(): Flow<Boolean> = observeAuthState()
        .map { session -> session is AuthSession.SignedIn }
}
