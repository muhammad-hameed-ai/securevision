plugins {
    alias(libs.plugins.securevision.android.library)
    alias(libs.plugins.securevision.android.hilt)
}

android {
    namespace = "com.securevision.ml.motion"
}

dependencies {
    implementation(projects.core.coreDomain)

    // Motion is derived from frame differencing, not a neural network, so this
    // module needs the camera frame types but no TFLite runtime.
    implementation(libs.androidx.camera.core)
}
