plugins {
    alias(libs.plugins.securevision.android.library)
    alias(libs.plugins.securevision.android.hilt)
}

android {
    namespace = "com.securevision.ml.face"

    /*
     * TFLite memory-maps its model file, which requires the asset to be stored
     * uncompressed. Declared here for intent, but note it only actually takes
     * effect at APK packaging time — see the same setting in the application
     * convention plugin, which is the one that matters.
     */
    androidResources {
        noCompress += "tflite"
    }
}

dependencies {
    implementation(projects.core.coreDomain)

    // Detection and five-point landmarks.
    implementation(libs.mlkit.face.detection)

    // Embedding, with the GPU delegate for real-time throughput.
    implementation(libs.bundles.tensorflow.lite)

    implementation(libs.androidx.camera.core)
}
