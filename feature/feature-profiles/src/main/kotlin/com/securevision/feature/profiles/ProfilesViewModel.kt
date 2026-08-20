package com.securevision.feature.profiles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securevision.core.common.result.getOrDefault
import com.securevision.core.domain.usecase.profile.DeleteProfileUseCase
import com.securevision.core.domain.usecase.profile.GetEnrolledProfilesUseCase
import com.securevision.core.model.EnrolledProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Drives the enrolled-profiles list.
 *
 * Filtering happens here, over a single subscription to the repository stream,
 * rather than by re-querying per keystroke. Re-subscribing would restart the Flow
 * and make the list blink on every character typed; the whole set is small enough
 * that filtering it in memory costs nothing.
 */
@HiltViewModel
class ProfilesViewModel @Inject constructor(
    getEnrolledProfiles: GetEnrolledProfilesUseCase,
    private val deleteProfile: DeleteProfileUseCase,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val watchlistOnly = MutableStateFlow(false)

    private val _pendingDeletion = MutableStateFlow<EnrolledProfile?>(null)

    /** The profile awaiting delete confirmation, if any. */
    val pendingDeletion: StateFlow<EnrolledProfile?> = _pendingDeletion.asStateFlow()

    /** Current screen state. */
    val uiState: StateFlow<ProfilesUiState> = combine(
        getEnrolledProfiles(GetEnrolledProfilesUseCase.Params()).map { result ->
            result.getOrDefault(emptyList())
        },
        query,
        watchlistOnly,
    ) { profiles, searchText, onlyWatchlisted ->
        if (profiles.isEmpty()) {
            ProfilesUiState.Empty()
        } else {
            ProfilesUiState.Content(
                profiles = profiles.filtered(searchText, onlyWatchlisted),
                query = searchText,
                watchlistOnly = onlyWatchlisted,
                totalCount = profiles.size,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        // Kept warm briefly across a rotation so the list does not flash Loading
        // on every configuration change.
        started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MILLIS),
        initialValue = ProfilesUiState.Loading,
    )

    /**
     * Updates the search text.
     *
     * @param text What the user has typed; blank clears the filter.
     */
    fun onQueryChange(text: String) {
        query.value = text
    }

    /** Toggles the watchlist-only filter. It composes with search rather than replacing it. */
    fun onWatchlistFilterToggle() {
        watchlistOnly.value = !watchlistOnly.value
    }

    /**
     * Asks for confirmation before removing someone.
     *
     * @param profile The profile the user tapped delete on.
     */
    fun onDeleteRequested(profile: EnrolledProfile) {
        _pendingDeletion.value = profile
    }

    /** Abandons a pending deletion. */
    fun onDeleteCancelled() {
        _pendingDeletion.value = null
    }

    /**
     * Deletes the profile awaiting confirmation.
     *
     * No undo is offered. The embedding is biometric data held nowhere else, so an
     * undo would have to keep a copy of exactly the thing the user asked to
     * destroy.
     */
    fun onDeleteConfirmed() {
        val target = _pendingDeletion.value ?: return
        _pendingDeletion.value = null

        viewModelScope.launch {
            deleteProfile(DeleteProfileUseCase.Params(target.id))
        }
    }

    private fun List<EnrolledProfile>.filtered(
        searchText: String,
        onlyWatchlisted: Boolean,
    ): List<EnrolledProfile> {
        val trimmed = searchText.trim()

        return filter { profile ->
            (!onlyWatchlisted || profile.isWatchlisted) &&
                (trimmed.isEmpty() || profile.name.contains(trimmed, ignoreCase = true))
        }
    }

    private companion object {
        const val SUBSCRIPTION_TIMEOUT_MILLIS = 5_000L
    }
}
