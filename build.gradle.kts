/*
 * Root build script.
 *
 * Every plugin is declared here with `apply false`. That places it on the build's
 * classpath — which is what lets the convention plugins in `build-logic` apply
 * them by id — without applying anything to the root project itself.
 *
 * No module build script may declare a plugin version. They all come from
 * `gradle/libs.versions.toml`.
 */
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.google.services) apply false
}
