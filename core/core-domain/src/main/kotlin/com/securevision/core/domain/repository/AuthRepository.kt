package com.securevision.core.domain.repository

import com.securevision.core.model.AccountCreation
import com.securevision.core.model.AuthSession
import com.securevision.core.model.UserAccount
import kotlinx.coroutines.flow.Flow

/**
 * The app-login account, stored entirely on this device.
 *
 * No network, no cloud copy. Only a BCrypt hash of the password is persisted;
 * the plaintext exists for the duration of a call and never reaches storage, a
 * log, or the domain model.
 *
 * Implementations **throw**
 * [com.securevision.core.domain.usecase.auth.AuthValidationException] on failure
 * rather than returning a result type. Callers reach this through a use case,
 * whose base class converts the throw into `Result.Error` carrying the reason —
 * so the presentation layer still receives a precise, localisable failure.
 */
interface AuthRepository {

    /**
     * Creates the single operator account and signs it in.
     *
     * @param username Unique handle.
     * @param fullName Account holder's display name.
     * @param cnic National identity number, with or without separators.
     * @param password Plain-text password; hashed before storage.
     * @return The account together with its one-time recovery code.
     * @throws com.securevision.core.domain.usecase.auth.AuthValidationException
     *   with `ACCOUNT_ALREADY_EXISTS` if an account is already present — this is
     *   a single-operator app — or `USERNAME_TAKEN`.
     */
    suspend fun signUp(
        username: String,
        fullName: String,
        cnic: String,
        password: String,
    ): AccountCreation

    /**
     * Signs in an existing account and persists the session.
     *
     * @param username Handle chosen at sign-up.
     * @param password Plain-text password, verified against the stored hash.
     * @return The signed-in account.
     * @throws com.securevision.core.domain.usecase.auth.AuthValidationException
     *   with `INVALID_CREDENTIALS` when the username is unknown or the password
     *   does not verify. Deliberately the same reason for both, so the error
     *   cannot be used to discover whether a username exists.
     */
    suspend fun login(username: String, password: String): UserAccount

    /** Clears the persisted session. The account itself is untouched. */
    suspend fun logout()

    /**
     * Sets a new password using the recovery code issued at sign-up.
     *
     * The only route back into an offline account whose password has been
     * forgotten. Does not sign the user in — they log in with the new password,
     * which proves it was set as intended.
     *
     * @param username Handle of the account to recover.
     * @param recoveryCode The code shown once at sign-up.
     * @param newPassword Replacement password.
     * @return The account whose password was changed.
     * @throws com.securevision.core.domain.usecase.auth.AuthValidationException
     *   with `INVALID_RECOVERY_CODE` when the username or code does not match.
     */
    suspend fun resetPassword(
        username: String,
        recoveryCode: String,
        newPassword: String,
    ): UserAccount

    /**
     * The launch gate: whether an account exists, and whether it is signed in.
     *
     * A stream rather than a one-shot read, so a sign-out anywhere in the app
     * propagates without the shell needing to be told.
     */
    fun observeAuthState(): Flow<AuthSession>

    /** The signed-in account, or `null` when signed out or no account exists. */
    fun getCurrentUser(): Flow<UserAccount?>

    /** Whether a session is currently active. */
    fun isLoggedIn(): Flow<Boolean>
}
