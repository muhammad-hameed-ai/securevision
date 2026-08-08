plugins {
    alias(libs.plugins.securevision.android.library)
    alias(libs.plugins.securevision.android.hilt)
}

android {
    namespace = "com.securevision.ml.object"
}

dependencies {
    implementation(projects.core.coreDomain)

    implementation(libs.bundles.tensorflow.lite)
    implementation(libs.androidx.camera.core)
}
