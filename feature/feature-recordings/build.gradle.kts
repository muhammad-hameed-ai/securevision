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

    implementation(libs.coil.compose)
    implementation(libs.coil.video)
}

/*
 * Media3 is deliberately absent until Phase 6 writes the player.
 *
 * The bundle is still in the version catalog, but declaring it here before
 * anything uses it put `media3-common` on the app's classpath, and its manifest
 * contributes ACCESS_NETWORK_STATE to the merged result. A permission the user
 * can see in the store listing is not an acceptable price for a dependency no
 * code calls yet. Re-add `libs.bundles.media3` alongside the player itself.
 */
