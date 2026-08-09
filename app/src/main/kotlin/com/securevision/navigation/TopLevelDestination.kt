package com.securevision.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.ui.graphics.vector.ImageVector
import com.securevision.R

/**
 * The destinations reachable from the navigation drawer.
 *
 * A subset of [SecureVisionRoute]: the authentication screens are deliberately
 * absent, because the drawer only exists once an account is signed in.
 *
 * @property destination The route this entry navigates to.
 * @property selectedIcon Icon shown while this entry is the current destination.
 * @property unselectedIcon Icon shown otherwise.
 * @property labelRes Localised drawer label.
 */
enum class TopLevelDestination(
    val destination: SecureVisionRoute,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    @param:StringRes val labelRes: Int,
) {
    DASHBOARD(
        destination = SecureVisionRoute.Dashboard,
        selectedIcon = Icons.Filled.Dashboard,
        unselectedIcon = Icons.Outlined.Dashboard,
        labelRes = R.string.destination_dashboard,
    ),
    LIVE(
        destination = SecureVisionRoute.Live,
        selectedIcon = Icons.Filled.Videocam,
        unselectedIcon = Icons.Outlined.Videocam,
        labelRes = R.string.destination_live,
    ),
    ALERTS(
        destination = SecureVisionRoute.Alerts,
        selectedIcon = Icons.Filled.NotificationsActive,
        unselectedIcon = Icons.Outlined.NotificationsActive,
        labelRes = R.string.destination_alerts,
    ),
    PROFILES(
        destination = SecureVisionRoute.Profiles,
        selectedIcon = Icons.Filled.People,
        unselectedIcon = Icons.Outlined.People,
        labelRes = R.string.destination_profiles,
    ),
    RECORDINGS(
        destination = SecureVisionRoute.Recordings,
        selectedIcon = Icons.Filled.VideoLibrary,
        unselectedIcon = Icons.Outlined.VideoLibrary,
        labelRes = R.string.destination_recordings,
    ),
    HISTORY(
        destination = SecureVisionRoute.History,
        selectedIcon = Icons.Filled.History,
        unselectedIcon = Icons.Outlined.History,
        labelRes = R.string.destination_history,
    ),
    SETTINGS(
        destination = SecureVisionRoute.Settings,
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings,
        labelRes = R.string.destination_settings,
    ),

    /**
     * The operator's own account.
     *
     * Labelled "My Account" rather than "Profile", and placed last: the drawer
     * already contains **Profiles**, meaning the enrolled people the camera
     * recognises. Two adjacent entries one letter apart, meaning entirely
     * different things, is precisely the confusion this product cannot afford.
     */
    MY_ACCOUNT(
        destination = SecureVisionRoute.Profile,
        selectedIcon = Icons.Filled.AccountCircle,
        unselectedIcon = Icons.Outlined.AccountCircle,
        labelRes = R.string.destination_my_account,
    );

    /** The navigation route string for this entry. */
    val route: String get() = destination.route

    companion object {
        /**
         * Finds the drawer entry matching a route, or `null` when the route is not
         * a top-level destination — which is how the shell decides whether to show
         * the drawer at all.
         *
         * @param route Route string to look up.
         */
        fun fromRoute(route: String?): TopLevelDestination? =
            entries.firstOrNull { it.route == route }
    }
}
