plugins {
    alias(libs.plugins.securevision.android.application)
    alias(libs.plugins.securevision.android.compose)
    alias(libs.plugins.securevision.android.hilt)
}

android {
    namespace = "com.securevision"

    defaultConfig {
        applicationId = "com.securevision"
    }
}

dependencies {
    // --- Core ---------------------------------------------------------------
    implementation(projects.core.coreModel)
    implementation(projects.core.coreCommon)
    implementation(projects.core.coreDomain)
    implementation(projects.core.coreUi)

    // The only module that sees both the domain contracts and their
    // implementations, which is what closes the Hilt graph here and nowhere else.
    implementation(projects.core.coreData)
    implementation(projects.core.coreAlerting)

    // --- Feature (presentation) ---------------------------------------------
    implementation(projects.feature.featureAuth)
    implementation(projects.feature.featureDashboard)
    implementation(projects.feature.featureLive)
    implementation(projects.feature.featureProfiles)
    implementation(projects.feature.featureAlerts)
    implementation(projects.feature.featureRecordings)
    implementation(projects.feature.featureHistory)
    implementation(projects.feature.featureSettings)

    // --- ML (on-device inference engines) -----------------------------------
    implementation(projects.ml.mlFace)
    implementation(projects.ml.mlObject)
    implementation(projects.ml.mlWeapon)
    implementation(projects.ml.mlMotion)
    implementation(projects.ml.mlAttributes)

    // --- Presentation plumbing ----------------------------------------------
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
}
