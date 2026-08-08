package com.securevision.buildlogic

import org.gradle.api.JavaVersion

/**
 * Build-wide constants applied by every SecureVision convention plugin.
 *
 * These values are declared exactly once. No module build script restates an
 * SDK level, a Java version or a test runner.
 */
internal object ProjectConfig {

    const val COMPILE_SDK = 34
    const val MIN_SDK = 26
    const val TARGET_SDK = 34

    const val VERSION_CODE = 1
    const val VERSION_NAME = "3.0.0"

    val JAVA_VERSION: JavaVersion = JavaVersion.VERSION_17

    /** Hilt-aware JUnit runner is introduced in a later phase; the stock runner is enough for now. */
    const val ANDROID_TEST_RUNNER = "androidx.test.runner.AndroidJUnitRunner"

    /** Filename the Google Services plugin looks for inside the application module. */
    const val GOOGLE_SERVICES_FILE = "google-services.json"
}
