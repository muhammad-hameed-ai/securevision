package com.securevision.feature.dashboard.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.securevision.feature.dashboard.DashboardRoutes
import com.securevision.feature.dashboard.DashboardScreen
import com.securevision.feature.dashboard.DashboardViewModel

/**
 * Registers the Dashboard destination.
 *
 * The feature owns its own graph entry, so `:app` composes features rather than
 * knowing how any of them are built.
 *
 * @param onNavigateToLive Opens Live View.
 * @param onNavigateToAlerts Opens the alerts list.
 * @param onNavigateToProfiles Opens the enrolled profiles list.
 * @param onNavigateToSettings Opens settings.
 */
fun NavGraphBuilder.dashboardScreen(
    onNavigateToLive: () -> Unit,
    onNavigateToAlerts: () -> Unit,
    onNavigateToProfiles: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    composable(route = DashboardRoutes.DASHBOARD) {
        DashboardRoute(
            onNavigateToLive = onNavigateToLive,
            onNavigateToAlerts = onNavigateToAlerts,
            onNavigateToProfiles = onNavigateToProfiles,
            onNavigateToSettings = onNavigateToSettings,
        )
    }
}

/**
 * Connects [DashboardViewModel] to the stateless [DashboardScreen].
 *
 * Collection is lifecycle-aware, so the database flows stop being observed while
 * the app is backgrounded instead of waking on every write.
 */
@Composable
private fun DashboardRoute(
    onNavigateToLive: () -> Unit,
    onNavigateToAlerts: () -> Unit,
    onNavigateToProfiles: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DashboardScreen(
        uiState = uiState,
        onNavigateToLive = onNavigateToLive,
        onNavigateToAlerts = onNavigateToAlerts,
        onNavigateToProfiles = onNavigateToProfiles,
        onNavigateToSettings = onNavigateToSettings,
    )
}
