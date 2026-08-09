package com.securevision

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securevision.core.common.extension.stateInWhileSubscribed
import com.securevision.core.common.result.Result
import com.securevision.core.common.result.fold
import com.securevision.core.domain.usecase.auth.LogoutUseCase
import com.securevision.core.domain.usecase.auth.ObserveAuthStateUseCase
import com.securevision.core.domain.usecase.invoke
import com.securevision.core.model.AuthSession
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Decides which screen the app opens on, and owns sign-out.
 *
 * The decision is derived from the domain auth stream rather than read once at
 * launch, so a sign-out from anywhere — the drawer, the account screen, a future
 * session expiry — returns the operator to the login screen without any of those
 * places needing to know about navigation.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    observeAuthState: ObserveAuthStateUseCase,
    private val logoutUseCase: LogoutUseCase,
) : ViewModel() {

    /** Current launch-gate decision. */
    val uiState: StateFlow<MainUiState> = observeAuthState()
        .map(Result<AuthSession>::toMainUiState)
        .stateInWhileSubscribed(
            scope = viewModelScope,
            initialValue = MainUiState.Loading,
        )

    /**
     * Clears the session.
     *
     * Performs no navigation. The shell observes [uiState] and moves the
     * operator, which is what lets the drawer and the account screen share one
     * sign-out path instead of each implementing their own.
     */
    fun logout() {
        viewModelScope.launch { logoutUseCase(Unit) }
    }
}

/**
 * Where the app should open.
 *
 * Four cases, matching the four the product actually has. Collapsing
 * [NeedsAccount] and [Unauthenticated] into one "signed out" would send a
 * first-time operator to a login screen they cannot possibly satisfy.
 */
sealed interface MainUiState {

    /** The session has not been read yet; show the splash. */
    data object Loading : MainUiState

    /** No account exists on this device; open on sign-up. */
    data object NeedsAccount : MainUiState

    /** An account exists but nobody is signed in; open on login. */
    data object Unauthenticated : MainUiState

    /**
     * An account is signed in; open on the dashboard.
     *
     * @property accountName Display name of the signed-in operator.
     */
    data class Authenticated(val accountName: String) : MainUiState
}

private fun Result<AuthSession>.toMainUiState(): MainUiState = fold(
    onSuccess = { session ->
        when (session) {
            AuthSession.Unknown -> MainUiState.Loading
            AuthSession.NoAccount -> MainUiState.NeedsAccount
            AuthSession.SignedOut -> MainUiState.Unauthenticated
            is AuthSession.SignedIn -> MainUiState.Authenticated(session.account.fullName)
        }
    },
    // A failed read resolves to signed out. If the session cannot be established,
    // signing in again is both the correct outcome and the only useful action.
    onError = { _, _ -> MainUiState.Unauthenticated },
    onLoading = { MainUiState.Loading },
)
