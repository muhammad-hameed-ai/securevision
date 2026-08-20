package com.securevision.feature.recordings.navigation

import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.securevision.feature.recordings.RecordingPlayerScreen
import com.securevision.feature.recordings.RecordingPlayerViewModel
import com.securevision.feature.recordings.RecordingsRoutes
import com.securevision.feature.recordings.RecordingsScreen
import com.securevision.feature.recordings.RecordingsViewModel

/**
 * Registers the recordings destinations.
 *
 * @param navController Used for forward navigation within the feature.
 */
fun NavGraphBuilder.recordingsGraph(navController: NavHostController) {
    composable(route = RecordingsRoutes.RECORDINGS) {
        val viewModel: RecordingsViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val pendingDeletion by viewModel.pendingDeletion.collectAsStateWithLifecycle()

        RecordingsScreen(
            uiState = uiState,
            pendingDeletion = pendingDeletion,
            onPlay = { id -> navController.navigate(RecordingsRoutes.player(id)) },
            onDeleteRequested = viewModel::onDeleteRequested,
            onDeleteConfirmed = viewModel::onDeleteConfirmed,
            onDeleteCancelled = viewModel::onDeleteCancelled,
        )
    }

    composable(
        route = RecordingsRoutes.PLAYER,
        arguments = listOf(
            navArgument(RecordingsRoutes.ARG_RECORDING_ID) { type = NavType.StringType },
        ),
    ) {
        // Its own ViewModel, which resolves the clip by id. Reading the gallery's
        // list here showed "unavailable" whenever the player opened before that
        // list had emitted.
        val viewModel: RecordingPlayerViewModel = hiltViewModel()
        val recording by viewModel.recording.collectAsStateWithLifecycle()
        val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

        RecordingPlayerScreen(
            filePath = recording?.filePath,
            isLoading = isLoading,
            onBack = { navController.popBackStack() },
        )
    }
}
