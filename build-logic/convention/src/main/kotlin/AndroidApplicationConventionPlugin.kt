import com.android.build.api.dsl.ApplicationExtension
import com.securevision.buildlogic.ProjectConfig
import com.securevision.buildlogic.configureKotlinAndroid
import com.securevision.buildlogic.libs
import com.securevision.buildlogic.library
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * `securevision.android.application` — the single Android application module.
 *
 * Owns SDK levels, versioning, build types and the release shrinking policy.
 */
class AndroidApplicationConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("com.android.application")
            apply("org.jetbrains.kotlin.android")
        }

        extensions.configure<ApplicationExtension> {
            configureKotlinAndroid(this)

            defaultConfig {
                targetSdk = ProjectConfig.TARGET_SDK
                versionCode = ProjectConfig.VERSION_CODE
                versionName = ProjectConfig.VERSION_NAME
                testInstrumentationRunner = ProjectConfig.ANDROID_TEST_RUNNER
            }

            buildFeatures {
                buildConfig = true
            }

            buildTypes {
                getByName("debug") {
                    isMinifyEnabled = false
                }
                getByName("release") {
                    isMinifyEnabled = true
                    isShrinkResources = true
                    proguardFiles(
                        getDefaultProguardFile("proguard-android-optimize.txt"),
                        "proguard-rules.pro",
                    )
                }
            }
        }

        dependencies {
            add("implementation", libs.library("androidx-core-ktx"))
            add("implementation", libs.library("androidx-lifecycle-runtime-ktx"))
            add("implementation", libs.library("kotlinx-coroutines-android"))
            add("androidTestImplementation", libs.library("androidx-test-ext-junit"))
            add("androidTestImplementation", libs.library("androidx-test-espresso-core"))
        }
    }
}
