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

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)

    // Robolectric supplies an Android Context on the JVM, so the in-memory Room
    // DAO test runs inside `gradlew build` rather than needing a connected device.
    testImplementation(libs.robolectric)
}

/*
 * Phase 3 adds the Firebase Auth and Firestore sources for the app-login account
 * — the only data in the project that leaves the device.
 */
