plugins {
    alias(libs.plugins.securevision.android.library)
    alias(libs.plugins.securevision.android.compose)
    alias(libs.plugins.securevision.android.hilt)
}

android {
    namespace = "com.securevision.feature.recordings"
}

dependencies {
    implementation(projects.core.coreDomain)
    implementation(projects.core.coreUi)

    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)

    // Playback of clips held in internal storage.
    implementation(libs.bundles.media3)

    // Poster frames decoded straight from the video, so no thumbnail files are
    // written — storage is the scarcest thing this feature consumes.
    implementation(libs.coil.compose)
    implementation(libs.coil.video)
}

/*
 * Media3 contributes ACCESS_NETWORK_STATE to the merged manifest. The app module
 * strips it with `tools:node="remove"`, since nothing here streams over a
 * network: every clip is a local file. Verify the permission list against a built
 * APK rather than trusting this comment.
 */
