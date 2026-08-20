package com.securevision.feature.profiles

/**
 * Navigation identity of the enrolled person profiles feature.
 *
 * These are the on-device profiles used for face recognition — not the
 * app-login account, which the auth feature owns.
 */
object ProfilesRoutes {

    /** List of everyone the app can recognise. */
    const val PROFILES = "profiles"

    /** Capture-and-enrol flow for a new person. */
    const val ENROL = "profiles/enrol"

    /** Argument carrying which profile is being edited. */
    const val ARG_PROFILE_ID = "profileId"

    /** Edit an existing person, optionally re-enrolling their face. */
    const val EDIT = "profiles/edit/{$ARG_PROFILE_ID}"

    /**
     * Builds the edit route for one person.
     *
     * @param profileId Which profile to open.
     */
    fun edit(profileId: String): String = "profiles/edit/$profileId"
}
