plugins {
    alias(libs.plugins.securevision.android.library)
    alias(libs.plugins.securevision.android.hilt)
}

android {
    namespace = "com.securevision.core.common"
}

dependencies {
    // `api` rather than `implementation`: DispatcherProvider exposes
    // CoroutineDispatcher in its public signature, so consumers must see it.
    api(libs.kotlinx.coroutines.core)
}
