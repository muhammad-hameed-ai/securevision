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
     * Moves from a route within the authentication flow to the dashboard.
     *
     * Clears the auth screens from the back stack so the system back button
     * cannot return to a login screen the user has already passed.
     */
    fun navigateToDashboardAfterSignIn() {
        navController.navigate(SecureVisionRoute.Dashboard.route) {
            popUpTo(SecureVisionRoute.Login.route) { inclusive = true }
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
