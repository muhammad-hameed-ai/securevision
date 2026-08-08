package com.securevision.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.securevision.R
import com.securevision.core.ui.preview.PreviewContainer
import com.securevision.core.ui.preview.ThemePreviews
import com.securevision.core.ui.theme.SecureVisionDimens
import com.securevision.navigation.TopLevelDestination

/**
 * Navigation drawer listing every top-level destination.
 *
 * A drawer rather than a bottom bar: seven destinations exceeds what Material 3
 * recommends for a `NavigationBar`, and the drawer header gives the SecureVision
 * wordmark a home.
 *
 * @param selected The destination currently shown, or `null` if none is.
 * @param onDestinationSelected Invoked with the chosen destination.
 * @param modifier Modifier applied to the sheet.
 */
@Composable
fun SecureVisionDrawer(
    selected: TopLevelDestination?,
    onDestinationSelected: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalDrawerSheet(
        modifier = modifier,
        drawerContainerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(vertical = SecureVisionDimens.spacingMedium),
        ) {
            DrawerHeader()

            HorizontalDivider(
                modifier = Modifier.padding(
                    horizontal = SecureVisionDimens.spacingMedium,
                    vertical = SecureVisionDimens.spacingMediumSmall,
                ),
                color = MaterialTheme.colorScheme.outlineVariant,
            )

            TopLevelDestination.entries.forEach { destination ->
                val isSelected = destination == selected

                NavigationDrawerItem(
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                    selected = isSelected,
                    label = {
                        Text(
                            text = stringResource(destination.labelRes),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = if (isSelected) {
                                destination.selectedIcon
                            } else {
                                destination.unselectedIcon
                            },
                            // Decorative: the adjacent label already names the destination.
                            contentDescription = null,
                        )
                    },
                    onClick = { onDestinationSelected(destination) },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
            }
        }
    }
}

/** Wordmark and tagline shown at the top of the drawer. */
@Composable
private fun DrawerHeader() {
    Column(
        modifier = Modifier.padding(
            horizontal = SecureVisionDimens.spacingLarge,
            vertical = SecureVisionDimens.spacingMedium,
        ),
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.app_tagline),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@ThemePreviews
@Composable
private fun SecureVisionDrawerPreview() {
    PreviewContainer {
        SecureVisionDrawer(
            selected = TopLevelDestination.DASHBOARD,
            onDestinationSelected = {},
        )
    }
}
