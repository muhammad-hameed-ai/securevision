package com.securevision.core.domain.usecase.profile

import com.securevision.core.domain.engine.FaceRecognitionEngine
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The architectural rule this whole pipeline depends on: **one embedding path**.
 *
 * A previous version of this app produced 0.23 similarity for everybody. The
 * cause was enrolment and recognition preparing faces differently. The fix was a
 * shared detect → align → embed path, and the way that fix survives future
 * changes is this test — a source scan asserting that
 * [FaceRecognitionEngine.embedForEnrolment] has exactly one call site.
 *
 * A source scan rather than a mock assertion, deliberately. A mock proves the one
 * use case behaves; only reading the tree proves nobody has added a second caller
 * somewhere else.
 */
class SingleEmbeddingPathTest {

    @Test
    fun `embedForEnrolment is called from exactly one place`() {
        val callSites = kotlinSources()
            // Invocations only. `fun embedForEnrolment(` is the interface
            // declaration and its override — those are the contract, not callers.
            .filter { file -> CALL_SITE.containsMatchIn(file.readText()) }
            .map { file -> file.name }
            .sorted()

        assertEquals(
            "embedForEnrolment must have exactly one call site. Found: $callSites. " +
                "Adding a second embedding path is what causes uniformly low match scores.",
            listOf(EXPECTED_CALL_SITE),
            callSites,
        )
    }

    @Test
    fun `the engine exposes only one enrolment embedding entry point`() {
        val engineSource = kotlinSources()
            .first { file -> file.name == "FaceRecognitionEngine.kt" }
            .readText()

        val declarations = Regex("""fun\s+\w*[Ee]mbed\w*\s*\(""")
            .findAll(engineSource)
            .map { match -> match.value }
            .toList()

        assertEquals(
            "The engine should declare exactly one embedding function. Found: $declarations",
            1,
            declarations.size,
        )
    }

    private fun kotlinSources(): List<File> {
        // Walks the whole repository rather than this module: a second call site
        // added in a feature or ml module is exactly the case worth catching.
        val root = generateSequence(File("").absoluteFile) { it.parentFile }
            .first { candidate -> File(candidate, SETTINGS_FILE).exists() }

        val sources = root.walkTopDown()
            .filter { file -> file.isFile && file.extension == "kt" }
            .filterNot { file -> file.path.contains(BUILD_DIRECTORY) }
            .filterNot { file -> file.name == "SingleEmbeddingPathTest.kt" }
            .toList()

        assertTrue("expected to find Kotlin sources under $root", sources.isNotEmpty())

        return sources
    }

    private companion object {
        const val EMBED_FUNCTION = "embedForEnrolment"

        /**
         * An invocation, not a declaration.
         *
         * Requires a receiver dot before the name, which every call has and no
         * `fun` declaration does.
         */
        val CALL_SITE = Regex("""\.\s*$EMBED_FUNCTION\s*\(""")

        /** The only class permitted to call the embedder for enrolment. */
        const val EXPECTED_CALL_SITE = "CaptureEnrolmentUseCase.kt"

        const val SETTINGS_FILE = "settings.gradle.kts"
        val BUILD_DIRECTORY = "${File.separator}build${File.separator}"
    }
}
