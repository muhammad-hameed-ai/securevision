package com.securevision.feature.alerts.navigation

import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.securevision.feature.alerts.AlertDetailScreen
import com.securevision.feature.alerts.AlertDetailViewModel
import com.securevision.feature.alerts.AlertsRoutes
import com.securevision.feature.alerts.AlertsScreen
import com.securevision.feature.alerts.AlertsViewModel

/**
 * Registers the alerts destinations.
 *
 * @param navController Used for forward navigation within the feature.
 */
fun NavGraphBuilder.alertsGraph(navController: NavHostController) {
    composable(route = AlertsRoutes.ALERTS) {
        val viewModel: AlertsViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val lastDismissed by viewModel.lastDismissed.collectAsStateWithLifecycle()

        AlertsScreen(
            uiState = uiState,
            lastDismissed = lastDismissed,
            onFilterChange = viewModel::onFilterChange,
            onMarkAllRead = viewModel::onMarkAllRead,
            onDismiss = viewModel::onDismiss,
            onUndoDismiss = viewModel::onUndoDismiss,
            onUndoExpired = viewModel::onUndoExpired,
            onOpenAlert = { id -> navController.navigate(AlertsRoutes.detail(id)) },
        )
    }

    composable(
        route = AlertsRoutes.DETAIL,
        arguments = listOf(navArgument(AlertsRoutes.ARG_ALERT_ID) { type = NavType.StringType }),
    ) {
        val viewModel: AlertDetailViewModel = hiltViewModel()
        val alert by viewModel.alert.collectAsStateWithLifecycle()
        val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

        AlertDetailScreen(
            alert = alert,
            isLoading = isLoading,
            onBack = { navController.popBackStack() },
        )
    }
}
