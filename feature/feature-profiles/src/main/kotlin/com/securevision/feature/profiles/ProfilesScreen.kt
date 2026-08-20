package com.securevision.feature.profiles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PersonAddAlt
import androidx.compose.material.icons.outlined.PeopleOutline
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.securevision.core.model.EnrolledProfile
import com.securevision.core.ui.component.SVChip
import com.securevision.core.ui.component.SVEmptyState
import com.securevision.core.ui.component.SVPrimaryButton
import com.securevision.core.ui.component.SVTextField
import com.securevision.core.ui.component.SVTopBar
import com.securevision.core.ui.theme.SecureVisionDimens
import com.securevision.feature.profiles.component.DeleteProfileDialog
import com.securevision.feature.profiles.component.ProfileCard

/**
 * Everyone the app can recognise.
 *
 * @param uiState What to render.
 * @param pendingDeletion Profile awaiting delete confirmation, if any.
 * @param onQueryChange Search text changed.
 * @param onWatchlistFilterToggle Watchlist-only filter toggled.
 * @param onAddProfile Opens enrolment.
 * @param onEditProfile Opens a profile for editing.
 * @param onDeleteRequested Asks to remove a profile.
 * @param onDeleteConfirmed Confirms removal.
 * @param onDeleteCancelled Abandons removal.
 * @param modifier Modifier applied to the screen.
 */
@Composable
fun ProfilesScreen(
    uiState: ProfilesUiState,
    pendingDeletion: EnrolledProfile?,
    onQueryChange: (String) -> Unit,
    onWatchlistFilterToggle: () -> Unit,
    onAddProfile: () -> Unit,
    onEditProfile: (String) -> Unit,
    onDeleteRequested: (EnrolledProfile) -> Unit,
    onDeleteConfirmed: () -> Unit,
    onDeleteCancelled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { SVTopBar(title = stringResource(R.string.profiles_title)) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddProfile,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(imageVector = Icons.Outlined.PersonAddAlt, contentDescription = null)
                Text(
                    text = stringResource(R.string.profiles_add),
                    modifier = Modifier.padding(start = SecureVisionDimens.spacingSmall),
                )
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (uiState) {
                ProfilesUiState.Loading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary,
                )

                is ProfilesUiState.Empty -> SVEmptyState(
                    icon = Icons.Outlined.PeopleOutline,
                    title = stringResource(R.string.profiles_empty_title),
                    subtitle = uiState.message
                        ?: stringResource(R.string.profiles_empty_subtitle),
                    modifier = Modifier.fillMaxSize(),
                    action = {
                        SVPrimaryButton(
                            text = stringResource(R.string.profiles_add),
                            onClick = onAddProfile,
                        )
                    },
                )

                is ProfilesUiState.Content -> ProfilesContent(
                    state = uiState,
                    onQueryChange = onQueryChange,
                    onWatchlistFilterToggle = onWatchlistFilterToggle,
                    onEditProfile = onEditProfile,
                    onDeleteRequested = onDeleteRequested,
                )
            }
        }
    }

    if (pendingDeletion != null) {
        DeleteProfileDialog(
            profileName = pendingDeletion.name,
            onConfirm = onDeleteConfirmed,
            onDismiss = onDeleteCancelled,
        )
    }
}

@Composable
private fun ProfilesContent(
    state: ProfilesUiState.Content,
    onQueryChange: (String) -> Unit,
    onWatchlistFilterToggle: () -> Unit,
    onEditProfile: (String) -> Unit,
    onDeleteRequested: (EnrolledProfile) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(
                horizontal = SecureVisionDimens.spacingMedium,
                vertical = SecureVisionDimens.spacingSmall,
            ),
            verticalArrangement = Arrangement.spacedBy(SecureVisionDimens.spacingSmall),
        ) {
            SVTextField(
                value = state.query,
                onValueChange = onQueryChange,
                label = stringResource(R.string.profiles_search_label),
                modifier = Modifier.fillMaxWidth(),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(SecureVisionDimens.spacingSmall)) {
                SVChip(
                    label = stringResource(R.string.profiles_filter_watchlist),
                    selected = state.watchlistOnly,
                    onClick = onWatchlistFilterToggle,
                )
            }
        }

        if (state.isFilteredEmpty) {
            // Distinct from "nobody enrolled": telling someone who is searching to
            // go and enrol people would be answering a question they did not ask.
            SVEmptyState(
                icon = Icons.Outlined.SearchOff,
                title = stringResource(R.string.profiles_no_matches_title),
                subtitle = stringResource(R.string.profiles_no_matches_subtitle),
                modifier = Modifier.fillMaxSize(),
            )
            return@Column
        }

        LazyColumn(
            contentPadding = PaddingValues(
                start = SecureVisionDimens.spacingMedium,
                end = SecureVisionDimens.spacingMedium,
                bottom = SecureVisionDimens.spacingHuge,
            ),
            verticalArrangement = Arrangement.spacedBy(SecureVisionDimens.spacingSmall),
        ) {
            items(items = state.profiles, key = EnrolledProfile::id) { profile ->
                ProfileCard(
                    profile = profile,
                    onClick = { onEditProfile(profile.id) },
                    onDelete = { onDeleteRequested(profile) },
                )
            }
        }
    }
}
