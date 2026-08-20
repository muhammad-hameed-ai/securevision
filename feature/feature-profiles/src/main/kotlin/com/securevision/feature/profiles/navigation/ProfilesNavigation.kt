package com.securevision.feature.profiles.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.securevision.feature.profiles.AddProfileScreen
import com.securevision.feature.profiles.AddProfileViewModel
import com.securevision.feature.profiles.ProfilesRoutes
import com.securevision.feature.profiles.ProfilesScreen
import com.securevision.feature.profiles.ProfilesViewModel

/**
 * Registers the profiles destinations.
 *
 * The feature owns its own graph entry, so `:app` composes the feature rather
 * than knowing how it is built.
 *
 * @param navController Used for forward navigation within the feature.
 */
fun NavGraphBuilder.profilesGraph(navController: NavHostController) {
    composable(route = ProfilesRoutes.PROFILES) {
        val viewModel: ProfilesViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val pendingDeletion by viewModel.pendingDeletion.collectAsStateWithLifecycle()

        ProfilesScreen(
            uiState = uiState,
            pendingDeletion = pendingDeletion,
            onQueryChange = viewModel::onQueryChange,
            onWatchlistFilterToggle = viewModel::onWatchlistFilterToggle,
            onAddProfile = { navController.navigate(ProfilesRoutes.ENROL) },
            onEditProfile = { id -> navController.navigate(ProfilesRoutes.edit(id)) },
            onDeleteRequested = viewModel::onDeleteRequested,
            onDeleteConfirmed = viewModel::onDeleteConfirmed,
            onDeleteCancelled = viewModel::onDeleteCancelled,
        )
    }

    composable(route = ProfilesRoutes.ENROL) {
        EnrolmentDestination(profileId = null, navController = navController)
    }

    composable(
        route = ProfilesRoutes.EDIT,
        arguments = listOf(navArgument(ProfilesRoutes.ARG_PROFILE_ID) { type = NavType.StringType }),
    ) { entry ->
        EnrolmentDestination(
            profileId = entry.arguments?.getString(ProfilesRoutes.ARG_PROFILE_ID),
            navController = navController,
        )
    }
}

/**
 * Add and edit share one destination.
 *
 * They are the same form: editing pre-fills the fields and makes the face
 * optional. Two destinations would mean two copies of the wiring and two places
 * for the save behaviour to drift.
 */
@androidx.compose.runtime.Composable
private fun EnrolmentDestination(profileId: String?, navController: NavHostController) {
    val viewModel: AddProfileViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val saved by viewModel.saved.collectAsStateWithLifecycle()

    LaunchedEffect(profileId) { viewModel.load(profileId) }

    LaunchedEffect(saved) {
        if (saved) navController.popBackStack()
    }

    AddProfileScreen(
        uiState = uiState,
        onNameChange = viewModel::onNameChange,
        onAgeChange = viewModel::onAgeChange,
        onAccessLevelChange = viewModel::onAccessLevelChange,
        onWatchlistToggle = viewModel::onWatchlistToggle,
        onPhotoCaptured = viewModel::onPhotoCaptured,
        onRetakePhoto = viewModel::onRetakePhoto,
        onSave = viewModel::onSave,
        onBack = { navController.popBackStack() },
    )
}
