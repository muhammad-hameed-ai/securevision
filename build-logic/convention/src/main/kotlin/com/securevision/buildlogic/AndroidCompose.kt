package com.securevision.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension

/**
 * Turns on Compose for a module and wires the BOM-managed Compose dependency set.
 *
 * Every Compose artifact version is resolved by the BOM, so no Compose library
 * carries an explicit version anywhere in the project.
 */
internal fun Project.configureAndroidCompose(commonExtension: CommonExtension<*, *, *, *, *, *>) {
    commonExtension.apply {
        buildFeatures {
            compose = true
        }
    }

    val composeBom = libs.library("androidx-compose-bom")

    dependencies {
        add("implementation", platform(composeBom))
        add("implementation", libs.bundle("compose-core"))
        add("androidTestImplementation", platform(composeBom))
        add("androidTestImplementation", libs.library("androidx-compose-ui-test-junit4"))
        add("debugImplementation", libs.library("androidx-compose-ui-tooling"))
        add("debugImplementation", libs.library("androidx-compose-ui-test-manifest"))
    }

    extensions.configure<ComposeCompilerGradlePluginExtension> {
        // Opt-in recomposition diagnostics: ./gradlew assembleDebug -Psecurevision.composeMetrics=true
        val metricsEnabled = providers
            .gradleProperty("securevision.composeMetrics")
            .orNull
            .toBoolean()

        if (metricsEnabled) {
            metricsDestination.set(rootProject.layout.buildDirectory.dir("compose-metrics"))
            reportsDestination.set(rootProject.layout.buildDirectory.dir("compose-reports"))
        }
    }
}
