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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
 * Root of the SecureVision UI: launch gate, chrome, drawer and navigation graph.
 *
 * @param uiState Auth state deciding where the app opens.
 * @param onLogout Clears the session. Performs no navigation itself — the shell
 *   reacts to the resulting state change.
 * @param modifier Modifier applied to the shell.
 * @param navState Navigation state; injectable for tests and previews.
 */
@Composable
fun SecureVisionAppShell(
    uiState: MainUiState,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    navState: SecureVisionNavState = rememberSecureVisionNavState(),
) {
    // Resolved once, from the first state that is not Loading. Re-keying NavHost
    // on every auth change would tear down and rebuild the whole graph mid
    // transition; sign-in, sign-up and sign-out move through explicit navigation
    // instead.
    var startDestination by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState) {
        if (startDestination == null) {
            startDestination = when (uiState) {
                MainUiState.Loading -> null
                MainUiState.NeedsAccount -> SecureVisionRoute.SignUp.route
                MainUiState.Unauthenticated -> SecureVisionRoute.Login.route
                is MainUiState.Authenticated -> SecureVisionRoute.Dashboard.route
            }
        }
    }

    val resolvedStart = startDestination

    if (resolvedStart == null) {
        SecureVisionSplash(modifier = modifier)
        return
    }

    // Sends the operator back to the auth flow whenever the session ends, no
    // matter which screen ended it. Deliberately a no-op while they are already
    // in that flow, so a password reset in progress is not interrupted.
    LaunchedEffect(uiState) {
        when (uiState) {
            MainUiState.NeedsAccount ->
                navState.navigateToAuthIfElsewhere(SecureVisionRoute.SignUp)
            MainUiState.Unauthenticated ->
                navState.navigateToAuthIfElsewhere(SecureVisionRoute.Login)
            MainUiState.Loading, is MainUiState.Authenticated -> Unit
        }
    }

    ShellContent(
        navState = navState,
        startDestination = resolvedStart,
        onLogout = onLogout,
        modifier = modifier,
    )
}

/**
 * Drawer, top bar and navigation host.
 *
 * The drawer and top bar appear only on top-level destinations: the auth screens
 * are full-bleed, and offering a drawer there would let the operator walk past
 * the very gate that protects the app.
 */
@Composable
private fun ShellContent(
    navState: SecureVisionNavState,
    startDestination: String,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    val currentRoute = navState.currentRoute
    val currentTopLevel = navState.currentTopLevelDestination
    val showChrome = currentTopLevel != null

    // Observed at the shell so the badge is live wherever the user is, rather
    // than only refreshing when the Alerts screen happens to be composed.
    val shellViewModel: AppShellViewModel = hiltViewModel()
    val unreadAlerts by shellViewModel.unreadAlerts.collectAsStateWithLifecycle()

    ModalNavigationDrawer(
        modifier = modifier,
        drawerState = drawerState,
        gesturesEnabled = showChrome,
        drawerContent = {
            SecureVisionDrawer(
                selected = currentTopLevel,
                unreadAlerts = unreadAlerts,
                onDestinationSelected = { destination ->
                    coroutineScope.launch { drawerState.close() }
                    navState.navigateToTopLevel(destination)
                },
                onLogout = {
                    coroutineScope.launch { drawerState.close() }
                    onLogout()
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

                    currentRoute == SecureVisionRoute.ForgotPassword.route -> SVTopBar(
                        title = stringResource(R.string.destination_forgot_password),
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
