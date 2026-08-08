import com.google.devtools.ksp.gradle.KspExtension
import com.securevision.buildlogic.libs
import com.securevision.buildlogic.library
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * `securevision.android.room` — the on-device database.
 *
 * Schemas are exported to `<module>/schemas` and are expected to be committed:
 * they are the input to Room's automated migration tests.
 */
class AndroidRoomConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.google.devtools.ksp")

        extensions.configure<KspExtension> {
            arg("room.schemaLocation", layout.projectDirectory.dir("schemas").asFile.path)
            arg("room.generateKotlin", "true")
        }

        dependencies {
            add("implementation", libs.library("androidx-room-runtime"))
            add("implementation", libs.library("androidx-room-ktx"))
            add("ksp", libs.library("androidx-room-compiler"))
        }
    }
}
