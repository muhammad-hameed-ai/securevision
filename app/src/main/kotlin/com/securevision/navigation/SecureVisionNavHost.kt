package com.securevision.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.securevision.R
import com.securevision.feature.dashboard.navigation.dashboardScreen
import com.securevision.ui.PlaceholderScreen

/**
 * The application navigation graph.
 *
 * Every destination currently renders a [PlaceholderScreen]. Later phases replace
 * each `composable { }` body with the real screen from its feature module; the
 * graph shape, the routes and the back-stack behaviour do not change.
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
        composable(SecureVisionRoute.Login.route) {
            PlaceholderScreen(
                icon = Icons.Outlined.Lock,
                title = stringResource(R.string.destination_login),
                description = stringResource(R.string.placeholder_login),
                actionLabel = stringResource(R.string.action_continue_to_dashboard),
                onAction = navState::navigateToDashboardAfterSignIn,
            )
        }

        composable(SecureVisionRoute.SignUp.route) {
            PlaceholderScreen(
                icon = Icons.Outlined.PersonAdd,
                title = stringResource(R.string.destination_sign_up),
                description = stringResource(R.string.placeholder_sign_up),
            )
        }

        // The first real screen. Its route and graph entry are owned by
        // :feature:feature-dashboard, so :app composes the feature rather than
        // knowing how it is built.
        dashboardScreen(
            onNavigateToLive = { navState.navigateToTopLevel(TopLevelDestination.LIVE) },
            onNavigateToAlerts = { navState.navigateToTopLevel(TopLevelDestination.ALERTS) },
            onNavigateToProfiles = { navState.navigateToTopLevel(TopLevelDestination.PROFILES) },
            onNavigateToSettings = { navState.navigateToTopLevel(TopLevelDestination.SETTINGS) },
        )

        composable(SecureVisionRoute.Live.route) {
            PlaceholderScreen(
                icon = Icons.Outlined.Videocam,
                title = stringResource(R.string.destination_live),
                description = stringResource(R.string.placeholder_live),
            )
        }

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
