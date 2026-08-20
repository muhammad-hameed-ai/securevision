package com.securevision.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
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
import com.securevision.feature.alerts.navigation.alertsGraph
import com.securevision.feature.dashboard.navigation.dashboardScreen
import com.securevision.feature.live.navigation.liveScreen
import com.securevision.feature.profiles.navigation.profilesGraph
import com.securevision.feature.recordings.navigation.recordingsGraph
import com.securevision.feature.settings.navigation.settingsScreen
import com.securevision.BuildConfig
import com.securevision.ui.PlaceholderScreen

/**
 * The application navigation graph.
 *
 * Every destination except History and Settings is a real screen owned by its
 * feature module; those two still render a [PlaceholderScreen] and arrive in
 * Phase 7. The graph shape, routes and back-stack behaviour do not change when
 * they do.
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
            onNavigateToHistory = { navState.navigateToTopLevel(TopLevelDestination.HISTORY) },
            onNavigateToSettings = { navState.navigateToTopLevel(TopLevelDestination.SETTINGS) },
        )

        // --- Live camera and face recognition --------------------------------
        liveScreen()

        // --- Alerts, people and recordings -----------------------------------
        alertsGraph(navState.navController)

        profilesGraph(navState.navController)

        recordingsGraph(navState.navController)

        // --- Awaiting later phases -------------------------------------------
        composable(SecureVisionRoute.History.route) {
            PlaceholderScreen(
                icon = Icons.Outlined.History,
                title = stringResource(R.string.destination_history),
                description = stringResource(R.string.placeholder_history),
            )
        }

        settingsScreen(appVersion = BuildConfig.VERSION_NAME)
    }
}
