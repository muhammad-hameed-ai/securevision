package com.securevision

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securevision.core.common.extension.stateInWhileSubscribed
import com.securevision.core.common.result.Result
import com.securevision.core.common.result.fold
import com.securevision.core.domain.usecase.auth.ObserveSessionUseCase
import com.securevision.core.domain.usecase.invoke
import com.securevision.core.model.AuthSession
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

/**
 * Decides which destination the app opens on.
 *
 * The decision is derived from the domain session stream rather than read once
 * at launch, so a sign-out from anywhere in the app returns the user to the
 * login screen without the shell needing to know it happened.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    observeSessionUseCase: ObserveSessionUseCase,
) : ViewModel() {

    /** Current start-destination decision. */
    val uiState: StateFlow<MainUiState> = observeSessionUseCase()
        .map(Result<AuthSession>::toMainUiState)
        .stateInWhileSubscribed(
            scope = viewModelScope,
            initialValue = MainUiState.Loading,
        )
}

/**
 * What the app shell should show at launch.
 *
 * A failure to read the session resolves to [Unauthenticated] rather than an
 * error screen: if the session cannot be established, signing in again is both
 * the correct outcome and the only useful thing the user can do.
 */
sealed interface MainUiState {

    /** The persisted session has not been read yet; show the splash. */
    data object Loading : MainUiState

    /** No account is signed in; open on the login screen. */
    data object Unauthenticated : MainUiState

    /**
     * An account is signed in; open on the dashboard.
     *
     * @property accountName Display name of the signed-in account.
     */
    data class Authenticated(val accountName: String) : MainUiState
}

private fun Result<AuthSession>.toMainUiState(): MainUiState = fold(
    onSuccess = { session ->
        when (session) {
            AuthSession.Unknown -> MainUiState.Loading
            AuthSession.SignedOut -> MainUiState.Unauthenticated
            is AuthSession.SignedIn -> MainUiState.Authenticated(session.account.fullName)
        }
    },
    onError = { _, _ -> MainUiState.Unauthenticated },
    onLoading = { MainUiState.Loading },
)
