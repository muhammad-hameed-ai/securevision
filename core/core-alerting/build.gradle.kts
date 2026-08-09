plugins {
    alias(libs.plugins.securevision.android.library)
    alias(libs.plugins.securevision.android.hilt)
}

android {
    namespace = "com.securevision.core.alerting"
}

dependencies {
    implementation(projects.core.coreDomain)

    // Notification building only. Deliberately separate from core-data: alerting
    // needs neither Room nor DataStore, and putting platform notification code
    // there would drag the persistence stack into a pure platform concern.
    implementation(libs.androidx.core.ktx)
}
