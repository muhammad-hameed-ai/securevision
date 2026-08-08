package com.securevision.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/**
 * Applies the Android and Kotlin settings shared by every Android module:
 * SDK levels, Java 17 source/target compatibility, lint policy and the
 * standard unit-test dependency set.
 */
internal fun Project.configureKotlinAndroid(commonExtension: CommonExtension<*, *, *, *, *, *>) {
    commonExtension.apply {
        compileSdk = ProjectConfig.COMPILE_SDK

        defaultConfig {
            minSdk = ProjectConfig.MIN_SDK
        }

        compileOptions {
            sourceCompatibility = ProjectConfig.JAVA_VERSION
            targetCompatibility = ProjectConfig.JAVA_VERSION
        }

        lint {
            // A lint error fails the build. Warnings are reported but tolerated so
            // that a new AGP's added checks cannot break CI overnight.
            abortOnError = true
            warningsAsErrors = false
            checkDependencies = false
            htmlReport = true
            xmlReport = false
        }

        packaging {
            resources {
                excludes += setOf(
                    "/META-INF/{AL2.0,LGPL2.1}",
                    "/META-INF/DEPENDENCIES",
                    "/META-INF/LICENSE*",
                    "/META-INF/NOTICE*",
                )
            }
        }

        testOptions {
            unitTests {
                // Robolectric needs merged Android resources and a manifest to build
                // its Context. Harmless for modules that do not use it.
                isIncludeAndroidResources = true
                isReturnDefaultValues = true
            }
        }
    }

    configureKotlinCompiler()
    addUnitTestDependencies()
}

/**
 * Applies the Java and Kotlin settings shared by pure-JVM modules — used by
 * `core-model`, which must never see the Android Gradle Plugin.
 */
internal fun Project.configureKotlinJvm() {
    extensions.configure<JavaPluginExtension> {
        sourceCompatibility = ProjectConfig.JAVA_VERSION
        targetCompatibility = ProjectConfig.JAVA_VERSION
    }

    configureKotlinCompiler()
    addUnitTestDependencies()
}

/** Pins the Kotlin compiler to JVM 17 and enables the project-wide opt-ins. */
private fun Project.configureKotlinCompiler() {
    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
            freeCompilerArgs.addAll(
                "-opt-in=kotlin.RequiresOptIn",
            )
        }
    }

    tasks.withType<Test>().configureEach {
        useJUnit()
    }
}

/** JUnit4 + MockK + Turbine + coroutines-test, available in every module's `src/test`. */
private fun Project.addUnitTestDependencies() {
    dependencies {
        add("testImplementation", libs.bundle("unit-test"))
    }
}
