plugins {
    alias(libs.plugins.securevision.android.library)
    alias(libs.plugins.securevision.android.hilt)
}

android {
    namespace = "com.securevision.core.domain"
}

dependencies {
    // `api`: use case signatures expose both domain models and Result, so every
    // consumer of a use case needs these types on its compile classpath.
    api(projects.core.coreModel)
    api(projects.core.coreCommon)
}

/*
 * core-domain deliberately does NOT depend on core-data or any ml module. It
 * declares repository interfaces; implementations are bound in the application
 * component. That absence is what stops a use case reaching a DAO.
 */
