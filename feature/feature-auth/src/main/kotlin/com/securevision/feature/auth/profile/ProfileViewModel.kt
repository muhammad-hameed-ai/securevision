package com.securevision.feature.auth.profile

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securevision.core.common.extension.stateInWhileSubscribed
import com.securevision.core.common.result.Result
import com.securevision.core.domain.usecase.auth.GetCurrentUserUseCase
import com.securevision.core.domain.usecase.auth.LogoutUseCase
import com.securevision.core.domain.usecase.invoke
import com.securevision.core.model.UserAccount
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * What the account screen renders.
 *
 * There is no error case: the account is read from the local database, and the
 * only way it is absent is that nobody is signed in — which [SignedOut] already
 * expresses.
 */
@Immutable
sealed interface ProfileUiState {

    /** The account has not been read yet. */
    data object Loading : ProfileUiState

    /** Nobody is signed in. */
    data object SignedOut : ProfileUiState

    /**
     * The signed-in account.
     *
     * @property account Profile fields. Carries no password material.
     */
    data class Content(val account: UserAccount) : ProfileUiState
}

/** Drives the account screen and its logout action. */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    getCurrentUser: GetCurrentUserUseCase,
    private val logoutUseCase: LogoutUseCase,
) : ViewModel() {

    /** Current account state. */
    val uiState: StateFlow<ProfileUiState> = getCurrentUser()
        .map { result ->
            when (result) {
                is Result.Success -> result.data
                    ?.let(ProfileUiState::Content)
                    ?: ProfileUiState.SignedOut
                // A failed read is indistinguishable from signed out for this
                // screen's purposes, and sending the operator to Login is the
                // only useful outcome either way.
                is Result.Error -> ProfileUiState.SignedOut
                Result.Loading -> ProfileUiState.Loading
            }
        }
        .stateInWhileSubscribed(
            scope = viewModelScope,
            initialValue = ProfileUiState.Loading,
        )

    /**
     * Clears the session.
     *
     * No navigation happens here. The shell observes the auth state and reacts,
     * so a sign-out from anywhere lands everyone on the same screen.
     */
    fun logout() {
        viewModelScope.launch { logoutUseCase(Unit) }
    }
}
