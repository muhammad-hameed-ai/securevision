import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import com.securevision.buildlogic.configureAndroidCompose
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * `securevision.android.compose` — layers Jetpack Compose onto an application or
 * library module.
 *
 * Uses [org.gradle.api.plugins.PluginManager.withPlugin] rather than an immediate
 * `hasPlugin` check so that this plugin is order-independent inside a module's
 * `plugins { }` block.
 */
class AndroidComposeConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        // Kotlin 2.0 moved the Compose compiler out of AGP into its own plugin.
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

        pluginManager.withPlugin("com.android.application") {
            extensions.configure<ApplicationExtension> { configureAndroidCompose(this) }
        }

        pluginManager.withPlugin("com.android.library") {
            extensions.configure<LibraryExtension> { configureAndroidCompose(this) }
        }
    }
}
