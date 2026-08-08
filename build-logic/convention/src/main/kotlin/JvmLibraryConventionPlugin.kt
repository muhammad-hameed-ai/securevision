import com.securevision.buildlogic.configureKotlinJvm
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * `securevision.jvm.library` — a pure Kotlin/JVM module with no Android on the
 * classpath at all.
 *
 * This is what makes `core-model`'s purity a compiler guarantee rather than a
 * code-review convention: an accidental `android.*` or `androidx.*` import in a
 * domain model becomes an unresolved reference.
 */
class JvmLibraryConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.jvm")
        configureKotlinJvm()
    }
}
