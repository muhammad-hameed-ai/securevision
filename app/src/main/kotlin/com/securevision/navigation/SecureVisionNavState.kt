package com.securevision.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

/**
 * Navigation state and behaviour for the app shell.
 *
 * Keeps the back-stack rules in one place instead of scattering `navigate { }`
 * option blocks across composables, where a missing `launchSingleTop` quietly
 * becomes a duplicated destination.
 *
 * @property navController The controller this state drives.
 */
@Stable
class SecureVisionNavState(val navController: NavHostController) {

    /** Route of the destination currently on top of the back stack. */
    val currentRoute: String?
        @Composable get() = navController.currentBackStackEntryAsState().value?.destination?.route

    /** The drawer entry for the current destination, or `null` if it is not a top-level one. */
    val currentTopLevelDestination: TopLevelDestination?
        @Composable get() = TopLevelDestination.fromRoute(currentRoute)

    /**
     * Switches to a drawer destination.
     *
     * Pops back to the dashboard while saving state, so the drawer never grows an
     * unbounded back stack and returning to a section restores its scroll
     * position rather than rebuilding it.
     *
     * @param destination Drawer entry to open.
     */
    fun navigateToTopLevel(destination: TopLevelDestination) {
        navController.navigate(destination.route) {
            popUpTo(SecureVisionRoute.Dashboard.route) {
                saveState = true
                inclusive = false
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    /**
     * Moves from the authentication flow to the dashboard.
     *
     * Clears the entire back stack rather than popping to a named route, because
     * the operator may have arrived from Login, from Sign-up, or from Sign-up via
     * the recovery-code screen. Whichever it was, Back from the dashboard must
     * exit the app, never return to a screen they have already satisfied.
     */
    fun navigateToDashboardAfterAuth() {
        navController.navigate(SecureVisionRoute.Dashboard.route) {
            popUpTo(navController.graph.id) { inclusive = true }
            launchSingleTop = true
        }
    }

    /**
     * Sends a signed-out operator back to the authentication flow.
     *
     * No-ops when they are already somewhere in that flow, so a password reset in
     * progress is not interrupted by the very state that reset is meant to fix.
     *
     * @param route The auth destination to land on.
     */
    fun navigateToAuthIfElsewhere(route: SecureVisionRoute) {
        val current = navController.currentDestination?.route ?: return

        if (current in SecureVisionRoute.AUTH_ROUTES) return

        navController.navigate(route.route) {
            popUpTo(navController.graph.id) { inclusive = true }
            launchSingleTop = true
        }
    }

    /** Returns to the sign-in screen after a password reset. */
    fun navigateToLoginAfterReset() {
        navController.navigate(SecureVisionRoute.Login.route) {
            popUpTo(navController.graph.id) { inclusive = true }
            launchSingleTop = true
        }
    }

    /**
     * Opens a destination that is not part of the drawer, such as sign-up.
     *
     * @param route Destination to open.
     */
    fun navigateTo(route: SecureVisionRoute) {
        navController.navigate(route.route) { launchSingleTop = true }
    }

    /** Pops the back stack by one entry. */
    fun navigateUp() {
        navController.navigateUp()
    }
}

/**
 * Remembers a [SecureVisionNavState] across recompositions.
 *
 * @param navController Controller to drive; a new one is created by default.
 */
@Composable
fun rememberSecureVisionNavState(
    navController: NavHostController = rememberNavController(),
): SecureVisionNavState = remember(navController) { SecureVisionNavState(navController) }
