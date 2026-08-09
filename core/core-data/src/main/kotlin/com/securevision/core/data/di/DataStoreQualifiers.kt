package com.securevision.core.data.di

import javax.inject.Qualifier

/**
 * The preferences store holding [com.securevision.core.model.AppSettings].
 *
 * Qualified because the app now has two distinct `DataStore<Preferences>`
 * bindings, and Hilt cannot tell them apart by type alone.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SettingsPreferences

/**
 * The preferences store holding the signed-in session.
 *
 * Deliberately a different file from [SettingsPreferences]: clearing settings
 * must never sign the operator out, and logging out must never disturb the
 * recognition thresholds.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SessionPreferences
