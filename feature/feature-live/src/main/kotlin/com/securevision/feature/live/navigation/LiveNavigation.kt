package com.securevision.feature.live.navigation

import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.securevision.feature.live.LiveCameraScreen
import com.securevision.feature.live.LiveCameraViewModel
import com.securevision.feature.live.LiveRoutes

/**
 * Registers the live camera destination.
 *
 * The feature owns its own graph entry, so `:app` composes the feature rather
 * than knowing how it is built.
 */
fun NavGraphBuilder.liveScreen() {
    composable(route = LiveRoutes.LIVE) {
        val viewModel: LiveCameraViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val enrolmentEvent by viewModel.enrolmentEvent.collectAsStateWithLifecycle()

        LiveCameraScreen(
            uiState = uiState,
            enrolmentEvent = enrolmentEvent,
            onFrame = viewModel::onFrame,
            onFlipCamera = viewModel::flipCamera,
            onEnrol = viewModel::enrolCurrentFace,
            onSilenceAlarm = viewModel::silenceAlarm,
            onEnrolmentEventConsumed = viewModel::consumeEnrolmentEvent,
        )
    }
}
