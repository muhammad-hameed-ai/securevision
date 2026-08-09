package com.securevision.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.securevision.R
import com.securevision.feature.auth.navigation.forgotPasswordScreen
import com.securevision.feature.auth.navigation.loginScreen
import com.securevision.feature.auth.navigation.profileScreen
import com.securevision.feature.auth.navigation.signUpScreen
import com.securevision.feature.dashboard.navigation.dashboardScreen
import com.securevision.feature.live.navigation.liveScreen
import com.securevision.ui.PlaceholderScreen

/**
 * The application navigation graph.
 *
 * Authentication and the dashboard are real screens owned by their feature
 * modules; the rest still render a [PlaceholderScreen] and are replaced in later
 * phases. The graph shape, routes and back-stack behaviour do not change when
 * they are.
 *
 * @param navState Navigation state driving this host.
 * @param startDestination Route the graph opens on.
 * @param modifier Modifier applied to the host.
 */
@Composable
fun SecureVisionNavHost(
    navState: SecureVisionNavState,
    startDestination: String,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navState.navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        // --- Authentication --------------------------------------------------
        loginScreen(
            onAuthenticated = navState::navigateToDashboardAfterAuth,
            onForgotPassword = { navState.navigateTo(SecureVisionRoute.ForgotPassword) },
        )

        signUpScreen(onSignUpComplete = navState::navigateToDashboardAfterAuth)

        forgotPasswordScreen(onPasswordReset = navState::navigateToLoginAfterReset)

        profileScreen()

        // --- Dashboard -------------------------------------------------------
        dashboardScreen(
            onNavigateToLive = { navState.navigateToTopLevel(TopLevelDestination.LIVE) },
            onNavigateToAlerts = { navState.navigateToTopLevel(TopLevelDestination.ALERTS) },
            onNavigateToProfiles = { navState.navigateToTopLevel(TopLevelDestination.PROFILES) },
            onNavigateToSettings = { navState.navigateToTopLevel(TopLevelDestination.SETTINGS) },
        )

        // --- Live camera and face recognition --------------------------------
        liveScreen()

        // --- Awaiting later phases -------------------------------------------
        composable(SecureVisionRoute.Alerts.route) {
            PlaceholderScreen(
                icon = Icons.Outlined.NotificationsActive,
                title = stringResource(R.string.destination_alerts),
                description = stringResource(R.string.placeholder_alerts),
            )
        }

        composable(SecureVisionRoute.Profiles.route) {
            PlaceholderScreen(
                icon = Icons.Outlined.People,
                title = stringResource(R.string.destination_profiles),
                description = stringResource(R.string.placeholder_profiles),
            )
        }

        composable(SecureVisionRoute.Recordings.route) {
            PlaceholderScreen(
                icon = Icons.Outlined.VideoLibrary,
                title = stringResource(R.string.destination_recordings),
                description = stringResource(R.string.placeholder_recordings),
            )
        }

        composable(SecureVisionRoute.History.route) {
            PlaceholderScreen(
                icon = Icons.Outlined.History,
                title = stringResource(R.string.destination_history),
                description = stringResource(R.string.placeholder_history),
            )
        }

        composable(SecureVisionRoute.Settings.route) {
            PlaceholderScreen(
                icon = Icons.Outlined.Settings,
                title = stringResource(R.string.destination_settings),
                description = stringResource(R.string.placeholder_settings),
            )
        }
    }
}
