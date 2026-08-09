plugins {
    alias(libs.plugins.securevision.android.library)
    alias(libs.plugins.securevision.android.hilt)
}

android {
    namespace = "com.securevision.ml.weapon"

    /*
     * Declared for intent; the setting that actually governs APK packaging is the
     * identical one in the application convention plugin.
     */
    androidResources {
        noCompress += "tflite"
    }
}

dependencies {
    implementation(projects.core.coreDomain)

    implementation(libs.bundles.tensorflow.lite)
    implementation(libs.androidx.camera.core)
}
