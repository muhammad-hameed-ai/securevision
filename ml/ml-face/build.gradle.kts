plugins {
    alias(libs.plugins.securevision.android.library)
    alias(libs.plugins.securevision.android.hilt)
}

android {
    namespace = "com.securevision.ml.face"
}

dependencies {
    implementation(projects.core.coreDomain)

    // Detection and five-point landmarks.
    implementation(libs.mlkit.face.detection)

    // FaceNet-512 embedding, with the GPU delegate for real-time throughput.
    implementation(libs.bundles.tensorflow.lite)

    implementation(libs.androidx.camera.core)
}
