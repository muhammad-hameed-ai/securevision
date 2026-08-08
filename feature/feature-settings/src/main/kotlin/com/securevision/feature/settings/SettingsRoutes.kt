package com.securevision.feature.settings

/**
 * Navigation identity of the settings feature.
 *
 * Phase 7 adds the editors for every field of
 * [com.securevision.core.model.AppSettings], including the recognition
 * threshold, match margin and voting frame count — the three values that
 * trade recognition sensitivity against false positives.
 */
object SettingsRoutes {

    /** Preferences and account destination. */
    const val SETTINGS = "settings"
}
