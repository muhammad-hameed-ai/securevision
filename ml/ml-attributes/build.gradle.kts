plugins {
    alias(libs.plugins.securevision.android.library)
    alias(libs.plugins.securevision.android.hilt)
}

android {
    namespace = "com.securevision.ml.attributes"

    androidResources {
        noCompress += "tflite"
    }
}

dependencies {
    implementation(projects.core.coreDomain)

    // Operates on the aligned crop the recognition pipeline already produced, so
    // this module needs no detector of its own.
    implementation(libs.bundles.tensorflow.lite)
}
