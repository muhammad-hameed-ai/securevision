package com.securevision.buildlogic

import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.artifacts.ExternalModuleDependencyBundle
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType

/**
 * Access to the `libs` version catalog from inside a convention plugin.
 *
 * Convention plugins run outside the `build.gradle.kts` script scope, so the
 * generated `libs` accessor is unavailable and the catalog must be looked up.
 */
internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

/**
 * Resolves a library alias, failing loudly with the offending alias name rather
 * than an opaque `NoSuchElementException` if the catalog entry is missing.
 */
internal fun VersionCatalog.library(alias: String): Provider<MinimalExternalModuleDependency> =
    findLibrary(alias).orElseThrow {
        IllegalStateException("Version catalog has no library alias '$alias'.")
    }

/** Resolves a bundle alias, with the same fail-loud behaviour as [library]. */
internal fun VersionCatalog.bundle(alias: String): Provider<ExternalModuleDependencyBundle> =
    findBundle(alias).orElseThrow {
        IllegalStateException("Version catalog has no bundle alias '$alias'.")
    }
