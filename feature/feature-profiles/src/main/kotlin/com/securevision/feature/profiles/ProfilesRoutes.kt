package com.securevision.feature.profiles

/**
 * Navigation identity of the enrolled person profiles feature.
 *
 * These are the on-device profiles used for face recognition — not the
 * app-login account, which the auth feature owns.
 *
 * Phase 3 adds the profile list and `ProfileEnrolmentScreen`, where a captured
 * photo is aligned and embedded before being stored.
 */
object ProfilesRoutes {

    /** List of everyone the app can recognise. */
    const val PROFILES = "profiles"

    /** Capture-and-enrol flow for a new person. */
    const val ENROL = "profiles/enrol"
}
