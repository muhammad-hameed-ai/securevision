package com.securevision.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.securevision.MainUiState
import com.securevision.R
import com.securevision.core.ui.component.SVTopBar
import com.securevision.core.ui.theme.SecureVisionDimens
import com.securevision.navigation.SecureVisionNavHost
import com.securevision.navigation.SecureVisionNavState
import com.securevision.navigation.SecureVisionRoute
import com.securevision.navigation.rememberSecureVisionNavState
import kotlinx.coroutines.launch

/**
 * Root of the SecureVision UI: chrome, drawer and navigation graph.
 *
 * The start destination is chosen from [uiState] once, when the session has been
 * read. Phase 3 will drive a mid-session sign-in or sign-out through navigation
 * rather than by swapping the start destination, which would rebuild the graph.
 *
 * @param uiState Session state deciding where the app opens.
 * @param modifier Modifier applied to the shell.
 * @param navState Navigation state; injectable for tests and previews.
 */
@Composable
fun SecureVisionAppShell(
    uiState: MainUiState,
    modifier: Modifier = Modifier,
    navState: SecureVisionNavState = rememberSecureVisionNavState(),
) {
    when (uiState) {
        MainUiState.Loading -> SecureVisionSplash(modifier = modifier)

        MainUiState.Unauthenticated -> ShellContent(
            navState = navState,
            startDestination = SecureVisionRoute.Login.route,
            modifier = modifier,
        )

        is MainUiState.Authenticated -> ShellContent(
            navState = navState,
            startDestination = SecureVisionRoute.Dashboard.route,
            modifier = modifier,
        )
    }
}

/**
 * Drawer, top bar and navigation host.
 *
 * The drawer and top bar appear only on top-level destinations: the
 * authentication screens are full-bleed, and offering a drawer there would let
 * the user walk past the login screen.
 */
@Composable
private fun ShellContent(
    navState: SecureVisionNavState,
    startDestination: String,
    modifier: Modifier = Modifier,
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    val currentRoute = navState.currentRoute
    val currentTopLevel = navState.currentTopLevelDestination
    val showChrome = currentTopLevel != null

    ModalNavigationDrawer(
        modifier = modifier,
        drawerState = drawerState,
        gesturesEnabled = showChrome,
        drawerContent = {
            SecureVisionDrawer(
                selected = currentTopLevel,
                onDestinationSelected = { destination ->
                    coroutineScope.launch { drawerState.close() }
                    navState.navigateToTopLevel(destination)
                },
            )
        },
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                when {
                    currentTopLevel != null -> SVTopBar(
                        title = stringResource(currentTopLevel.labelRes),
                        navigationIcon = {
                            IconButton(
                                onClick = { coroutineScope.launch { drawerState.open() } },
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Menu,
                                    contentDescription = stringResource(
                                        R.string.content_description_open_navigation_drawer,
                                    ),
                                )
                            }
                        },
                    )

                    currentRoute == SecureVisionRoute.SignUp.route -> SVTopBar(
                        title = stringResource(R.string.destination_sign_up),
                        onBack = navState::navigateUp,
                    )
                }
            },
        ) { contentPadding ->
            SecureVisionNavHost(
                navState = navState,
                startDestination = startDestination,
                modifier = Modifier.padding(contentPadding),
            )
        }
    }
}

/** Shown while the persisted session is being read. */
@Composable
private fun SecureVisionSplash(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.app_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = SecureVisionDimens.spacingSmall),
            )
            CircularProgressIndicator(
                modifier = Modifier.padding(top = SecureVisionDimens.spacingExtraLarge),
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
