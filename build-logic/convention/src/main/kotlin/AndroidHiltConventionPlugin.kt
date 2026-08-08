import com.securevision.buildlogic.libs
import com.securevision.buildlogic.library
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * `securevision.android.hilt` — dependency injection for a module.
 *
 * Uses KSP rather than KAPT: it is the supported path on Kotlin 2.0 and is
 * substantially faster on a build this wide.
 */
class AndroidHiltConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("com.google.devtools.ksp")
            apply("com.google.dagger.hilt.android")
        }

        dependencies {
            add("implementation", libs.library("hilt-android"))
            add("ksp", libs.library("hilt-compiler"))
        }

        // `hiltViewModel()` is only meaningful where Compose is present, so it is
        // added reactively instead of being forced on every module.
        pluginManager.withPlugin("org.jetbrains.kotlin.plugin.compose") {
            dependencies {
                add("implementation", libs.library("hilt-navigation-compose"))
            }
        }
    }
}
