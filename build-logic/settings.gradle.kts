/*
 * build-logic is an included (composite) build that produces SecureVision's
 * convention plugins. It reads the SAME version catalog as the main build, so
 * plugin versions and library versions can never drift apart.
 */

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "build-logic"

include(":convention")
