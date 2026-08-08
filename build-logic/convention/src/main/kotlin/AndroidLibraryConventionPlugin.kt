import com.android.build.api.dsl.LibraryExtension
import com.securevision.buildlogic.ProjectConfig
import com.securevision.buildlogic.configureKotlinAndroid
import com.securevision.buildlogic.libs
import com.securevision.buildlogic.library
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * `securevision.android.library` — every Android library module in the project.
 *
 * Library modules deliberately do NOT declare `targetSdk`; that is an
 * application-level concern and AGP ignores it for libraries.
 */
class AndroidLibraryConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("com.android.library")
            apply("org.jetbrains.kotlin.android")
        }

        extensions.configure<LibraryExtension> {
            configureKotlinAndroid(this)

            defaultConfig {
                testInstrumentationRunner = ProjectConfig.ANDROID_TEST_RUNNER
            }

            // Library modules never ship a release-only shrink step of their own;
            // shrinking happens once, in the application module. Modules that need
            // to publish consumer keep-rules (the ML modules, from Phase 4) declare
            // `consumerProguardFiles` in their own build script.
            buildTypes {
                getByName("release") {
                    isMinifyEnabled = false
                }
            }
        }

        dependencies {
            add("implementation", libs.library("kotlinx-coroutines-core"))
        }
    }
}
