import com.securevision.buildlogic.ProjectConfig
import com.securevision.buildlogic.libs
import com.securevision.buildlogic.library
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * `securevision.android.firebase` — Firebase Auth + Firestore.
 *
 * Scope note: Firebase backs the **app-login account only** (username, full name,
 * password, CNIC) so that it survives a reinstall. Enrolled person profiles and
 * every AI/ML workload stay on the device and never touch this dependency.
 *
 * The `com.google.gms.google-services` plugin hard-fails a build when
 * `google-services.json` is absent, and that file is intentionally git-ignored.
 * It is therefore applied only once the file is actually present, which keeps the
 * project buildable before Firebase has been provisioned.
 */
class AndroidFirebaseConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        val firebaseBom = libs.library("firebase-bom")

        dependencies {
            add("implementation", platform(firebaseBom))
            add("implementation", libs.library("firebase-auth"))
            add("implementation", libs.library("firebase-firestore"))
        }

        pluginManager.withPlugin("com.android.application") {
            val googleServicesConfig = file(ProjectConfig.GOOGLE_SERVICES_FILE)
            if (googleServicesConfig.exists()) {
                pluginManager.apply("com.google.gms.google-services")
            } else {
                logger.warn(
                    "SecureVision: ${ProjectConfig.GOOGLE_SERVICES_FILE} not found in " +
                        "${project.path}. Firebase Auth and Firestore are compiled in but " +
                        "inert — drop the file in to activate them.",
                )
            }
        }
    }
}
