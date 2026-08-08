plugins {
    alias(libs.plugins.securevision.android.library)
    alias(libs.plugins.securevision.android.compose)
    alias(libs.plugins.securevision.android.hilt)
}

android {
    namespace = "com.securevision.feature.alerts"
}

dependencies {
    implementation(projects.core.coreDomain)
    implementation(projects.core.coreUi)

    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)

    // Alert snapshots are rendered from on-device URIs.
    implementation(libs.coil.compose)
}
