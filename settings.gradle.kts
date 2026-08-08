pluginManagement {
    // Convention plugins live in a composite build so they compile against the
    // same version catalog the app resolves from.
    includeBuild("build-logic")

    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    // Modules may not declare their own repositories — every artifact resolves
    // through this single, auditable list.
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        google()
        mavenCentral()
    }
}

// Enables `projects.core.coreModel` style dependencies instead of stringly-typed
// `project(":core:core-model")`, so a renamed module fails at compile time.
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "SecureVision"

// --- Application entry point ------------------------------------------------
include(":app")

// --- Core: shared model, utilities, domain contracts, data, design system ----
include(":core:core-model")
include(":core:core-common")
include(":core:core-domain")
include(":core:core-data")
include(":core:core-ui")

// --- Feature: one module per screen area (presentation layer) ---------------
include(":feature:feature-auth")
include(":feature:feature-dashboard")
include(":feature:feature-live")
include(":feature:feature-profiles")
include(":feature:feature-alerts")
include(":feature:feature-recordings")
include(":feature:feature-history")
include(":feature:feature-settings")

// --- ML: on-device inference engines (data layer) ---------------------------
include(":ml:ml-face")
include(":ml:ml-object")
include(":ml:ml-weapon")
include(":ml:ml-motion")
include(":ml:ml-attributes")
