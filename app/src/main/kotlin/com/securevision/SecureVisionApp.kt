package com.securevision

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point and root of the Hilt object graph.
 *
 * Phase 5 adds notification channel creation here; for now the only job is to
 * bootstrap dependency injection.
 */
@HiltAndroidApp
class SecureVisionApp : Application()
