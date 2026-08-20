package com.securevision.feature.profiles

import androidx.compose.runtime.Immutable
import com.securevision.core.model.EnrolledProfile

/**
 * What the profiles list renders.
 *
 * [Empty] is distinct from `Content(emptyList())` on purpose: nobody enrolled at
 * all deserves an invitation to add someone, whereas a search that matched
 * nothing deserves "no results for that name". Collapsing the two produces a
 * screen that tells a searching user to start enrolling.
 */
@Immutable
sealed interface ProfilesUiState {

    /** The first load has not produced a list yet. */
    data object Loading : ProfilesUiState

    /**
     * Profiles are being shown.
     *
     * @property profiles The filtered list, newest first.
     * @property query Current search text.
     * @property watchlistOnly Whether the watchlist filter is on.
     * @property totalCount How many profiles exist before filtering, which is what
     *   distinguishes "no matches" from "none enrolled".
     */
    @Immutable
    data class Content(
        val profiles: List<EnrolledProfile>,
        val query: String = "",
        val watchlistOnly: Boolean = false,
        val totalCount: Int = profiles.size,
    ) : ProfilesUiState {

        /** Whether the filters are hiding everything, as opposed to nothing existing. */
        val isFilteredEmpty: Boolean get() = profiles.isEmpty() && totalCount > 0

        /** Whether any filter is active, so the UI can offer to clear it. */
        val isFiltered: Boolean get() = query.isNotBlank() || watchlistOnly
    }

    /**
     * Nobody is enrolled.
     *
     * @property message Optional explanation when loading failed rather than the
     *   list genuinely being empty.
     */
    data class Empty(val message: String? = null) : ProfilesUiState
}
