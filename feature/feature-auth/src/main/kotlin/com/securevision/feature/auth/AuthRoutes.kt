package com.securevision.feature.auth

/**
 * Navigation identity of the authentication feature.
 *
 * Routes live with the feature that owns them rather than in `:app`, so adding a
 * screen never requires editing the application module. `:app` composes these
 * into its graph.
 */
object AuthRoutes {

    /** Sign-in destination, shown when an account exists but no session does. */
    const val LOGIN = "auth/login"

    /**
     * Account creation, shown on first launch when no account exists.
     *
     * This destination also presents the one-time recovery code once the account
     * exists. It is a state swap within the same destination rather than a second
     * route on purpose: the code then never travels through a navigation
     * argument, and route strings end up in logs and in the back stack.
     */
    const val SIGN_UP = "auth/sign-up"

    /** Password reset using the recovery code. */
    const val FORGOT_PASSWORD = "auth/forgot-password"

    /** The signed-in operator's account details. Labelled "My Account" in the drawer. */
    const val PROFILE = "auth/account"
}
