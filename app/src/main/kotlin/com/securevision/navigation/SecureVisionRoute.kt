package com.securevision.navigation

import com.securevision.feature.alerts.AlertsRoutes
import com.securevision.feature.auth.AuthRoutes
import com.securevision.feature.dashboard.DashboardRoutes
import com.securevision.feature.history.HistoryRoutes
import com.securevision.feature.live.LiveRoutes
import com.securevision.feature.profiles.ProfilesRoutes
import com.securevision.feature.recordings.RecordingsRoutes
import com.securevision.feature.settings.SettingsRoutes

/**
 * Every destination in the SecureVision navigation graph.
 *
 * Each entry delegates to the route constant declared by the feature module that
 * owns the screen, so a feature can rename its own route without the application
 * module ever holding a second, stale copy of the string.
 *
 * @property route The navigation route string.
 */
sealed interface SecureVisionRoute {

    val route: String

    /** Sign-in screen. Shown first when no account is signed in. */
    data object Login : SecureVisionRoute {
        override val route: String = AuthRoutes.LOGIN
    }

    /** Account registration screen. */
    data object SignUp : SecureVisionRoute {
        override val route: String = AuthRoutes.SIGN_UP
    }

    /** Home screen once signed in. */
    data object Dashboard : SecureVisionRoute {
        override val route: String = DashboardRoutes.DASHBOARD
    }

    /** Real-time camera and detection. */
    data object Live : SecureVisionRoute {
        override val route: String = LiveRoutes.LIVE
    }

    /** Enrolled person profiles. */
    data object Profiles : SecureVisionRoute {
        override val route: String = ProfilesRoutes.PROFILES
    }

    /** Raised alerts. */
    data object Alerts : SecureVisionRoute {
        override val route: String = AlertsRoutes.ALERTS
    }

    /** Recorded clips. */
    data object Recordings : SecureVisionRoute {
        override val route: String = RecordingsRoutes.RECORDINGS
    }

    /** Detection history. */
    data object History : SecureVisionRoute {
        override val route: String = HistoryRoutes.HISTORY
    }

    /** Preferences and account. */
    data object Settings : SecureVisionRoute {
        override val route: String = SettingsRoutes.SETTINGS
    }
}
