package com.securevision.core.model

/**
 * Authentication state of the app-login account.
 *
 * Four cases rather than a nullable [UserAccount], because the app's launch gate
 * genuinely has four outcomes. A `UserAccount?` would collapse "no account has
 * ever been created" and "an account exists but nobody is signed in" into the
 * same `null`, yet those send the user to different screens — sign-up and login
 * respectively.
 *
 * The account is stored on-device only. There is no cloud copy, so this state is
 * derived entirely from the local database and the persisted session.
 */
sealed interface AuthSession {

    /**
     * The persisted session has not been read yet.
     *
     * Distinct from [SignedOut] so the shell can hold a splash instead of showing
     * the login screen for one frame before a valid session resolves.
     */
    data object Unknown : AuthSession

    /** No account has been created on this device; sign-up is the only route. */
    data object NoAccount : AuthSession

    /** An account exists but no session is active. */
    data object SignedOut : AuthSession

    /**
     * An account is signed in.
     *
     * @property account The signed-in account's profile fields. Carries no
     *   password material of any kind.
     */
    data class SignedIn(val account: UserAccount) : AuthSession
}
