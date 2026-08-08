plugins {
    alias(libs.plugins.securevision.android.library)
    alias(libs.plugins.securevision.android.hilt)
    alias(libs.plugins.securevision.android.room)
    alias(libs.plugins.securevision.android.firebase)
}

android {
    namespace = "com.securevision.core.data"
}

dependencies {
    // Implements the contracts declared in core-domain. Nothing depends on this
    // module except :app, which binds the implementations into the Hilt graph.
    implementation(projects.core.coreDomain)

    implementation(libs.androidx.datastore.preferences)
}

/*
 * Phase 2 fills this module in: Room entities, DAOs and the database; the
 * DataStore-backed settings source; and the on-device file store for enrolment
 * photos, snapshots and recordings.
 *
 * Phase 3 adds the Firebase Auth and Firestore sources for the app-login account
 * — the only data in the project that leaves the device.
 */
