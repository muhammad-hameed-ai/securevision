plugins {
    alias(libs.plugins.securevision.android.library)
    alias(libs.plugins.securevision.android.compose)
}

android {
    namespace = "com.securevision.core.ui"
}

dependencies {
    // `api`: SVSeverityBadge takes a domain Severity, so it appears in the
    // public signature of this module's components.
    api(projects.core.coreModel)

    implementation(libs.androidx.core.ktx)
}
