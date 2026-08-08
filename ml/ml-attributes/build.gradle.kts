plugins {
    alias(libs.plugins.securevision.android.library)
    alias(libs.plugins.securevision.android.hilt)
}

android {
    namespace = "com.securevision.ml.attributes"
}

dependencies {
    implementation(projects.core.coreDomain)

    // Reuses the aligned crop and landmarks produced by :ml:ml-face rather than
    // re-detecting, so attribute inference costs one extra pass, not two.
    implementation(libs.mlkit.face.detection)
    implementation(libs.bundles.tensorflow.lite)
}
