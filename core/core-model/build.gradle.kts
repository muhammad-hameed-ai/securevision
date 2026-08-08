plugins {
    alias(libs.plugins.securevision.jvm.library)
}

/*
 * core-model is a pure Kotlin/JVM module and has no dependencies at all — not
 * even coroutines. Every other module may depend on it; it depends on nothing.
 */
