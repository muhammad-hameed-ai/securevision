package com.securevision.feature.auth

/**
 * Navigation identity of the authentication feature.
 *
 * Routes live with the feature that owns them rather than in `:app`, so adding a
 * screen never requires editing the application module. `:app` composes these
 * constants into its graph.
 *
 * Phase 3 adds `LoginScreen`, `SignUpScreen`, their ViewModels and a
 * `NavGraphBuilder.authGraph()` extension alongside this file.
 */
object AuthRoutes {

    /** Destination shown when no account is signed in. */
    const val LOGIN = "auth/login"

    /** Account registration destination. */
    const val SIGN_UP = "auth/sign-up"
}
