package com.securevision.core.domain.repository

import com.securevision.core.model.AuthSession
import com.securevision.core.model.UserAccount
import kotlinx.coroutines.flow.Flow

/**
 * Access to the **app-login account** — the only cloud-backed entity in
 * SecureVision.
 *
 * Implemented against Firebase Auth and Firestore so an account survives an app
 * reinstall. Enrolled person profiles are a different concern entirely; see
 * [EnrolledProfileRepository].
 *
 * Implementations throw on failure; callers reach this through a use case, which
 * converts a throw into `Result.Error`.
 */
interface AuthRepository {

    /**
     * Registers a new account and signs it in.
     *
     * @param username Unique handle.
     * @param fullName Account holder's display name.
     * @param password Plain-text password; never stored by the app itself.
     * @param cnic National identity number.
     * @return The newly created account.
     */
    suspend fun signUp(
        username: String,
        fullName: String,
        password: String,
        cnic: String,
    ): UserAccount

    /**
     * Signs in an existing account.
     *
     * @param username Handle chosen at sign-up.
     * @param password Plain-text password.
     * @return The signed-in account.
     */
    suspend fun login(username: String, password: String): UserAccount

    /** Signs the current account out and clears the persisted session. */
    suspend fun logout()

    /**
     * The current session as a stream, so the app shell can react to a sign-out
     * that happens anywhere — including remotely.
     */
    fun getCurrentSession(): Flow<AuthSession>

    /** One-shot check used to choose the start destination at launch. */
    suspend fun isLoggedIn(): Boolean
}
