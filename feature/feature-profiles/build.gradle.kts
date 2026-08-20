plugins {
    alias(libs.plugins.securevision.android.library)
    alias(libs.plugins.securevision.android.compose)
    alias(libs.plugins.securevision.android.hilt)
}

android {
    namespace = "com.securevision.feature.profiles"
}

dependencies {
    implementation(projects.core.coreDomain)
    implementation(projects.core.coreUi)

    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)

    // The system photo picker, for enrolling from an existing photo. Needs no
    // storage permission, which is why it is used over a document picker.
    implementation(libs.androidx.activity.compose)

    // Enrolment photos are rendered from on-device URIs.
    implementation(libs.coil.compose)

    // Enrolment takes a still photo. The recognition engine it feeds is injected
    // as a domain contract, so no `ml` module is referenced from here.
    implementation(libs.bundles.camerax)
}
