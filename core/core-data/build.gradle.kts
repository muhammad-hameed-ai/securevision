plugins {
    alias(libs.plugins.securevision.android.library)
    alias(libs.plugins.securevision.android.hilt)
    alias(libs.plugins.securevision.android.room)
}

android {
    namespace = "com.securevision.core.data"
}

dependencies {
    // Implements the contracts declared in core-domain. Nothing depends on this
    // module except :app, which binds the implementations into the Hilt graph.
    implementation(projects.core.coreDomain)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)

    // Password and recovery-code hashing. Confined to this module: no hash and no
    // plaintext appears on a domain model, so nothing above the data layer needs it.
    implementation(libs.bcrypt)

    // Robolectric supplies an Android Context on the JVM, so the in-memory Room
    // DAO test runs inside `gradlew build` rather than needing a connected device.
    testImplementation(libs.robolectric)
}

/*
 * No network dependency of any kind. Authentication is BCrypt against a local
 * Room table, so every byte this module handles stays on the device.
 */
