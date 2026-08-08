package com.securevision.core.model

/**
 * Authentication state of the app-login account.
 *
 * Modelled as a closed hierarchy so that presentation code must handle every
 * case, and so "we do not know yet" is distinguishable from "signed out" —
 * the difference between showing a splash and showing the login screen.
 */
sealed interface AuthSession {

    /** The persisted session has not been read yet. */
    data object Unknown : AuthSession

    /** No account is signed in; the login screen is the correct destination. */
    data object SignedOut : AuthSession

    /**
     * An account is signed in.
     *
     * @property account The signed-in account's profile fields.
     */
    data class SignedIn(val account: UserAccount) : AuthSession
}
