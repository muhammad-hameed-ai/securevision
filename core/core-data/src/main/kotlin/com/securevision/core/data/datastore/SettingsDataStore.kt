package com.securevision.core.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.securevision.core.model.AppSettings
import com.securevision.core.model.CameraResolution
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * Preferences-backed store for [AppSettings].
 *
 * Every read falls back to the model's own default when a key is absent, so a
 * fresh install and a store written by an older version behave identically —
 * adding a setting in a later release never needs a migration.
 *
 * @property dataStore The underlying preferences store.
 */
@Singleton
class SettingsDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {

    /**
     * Current settings, re-emitted on every change.
     *
     * A corrupt or unreadable file surfaces as empty preferences rather than an
     * exception: losing preferences is recoverable, but a crash loop on startup
     * because of one bad byte is not.
     */
    val settingsFlow: Flow<AppSettings> = dataStore.data
        .catch { throwable ->
            if (throwable is IOException) emit(emptyPreferences()) else throw throwable
        }
        .map(Preferences::toAppSettings)

    /** @param threshold Minimum cosine similarity for a face match. */
    suspend fun setConfidenceThreshold(threshold: Float) = put(Keys.CONFIDENCE_THRESHOLD, threshold)

    /** @param margin Minimum lead the best match must hold over the runner-up. */
    suspend fun setMatchMargin(margin: Float) = put(Keys.MATCH_MARGIN, margin)

    /** @param frames Consecutive agreeing frames required to commit an identity. */
    suspend fun setVoteFrames(frames: Int) = put(Keys.VOTE_FRAMES, frames)

    /** @param enabled Detect and recognise faces. */
    suspend fun setFaceDetectionEnabled(enabled: Boolean) = put(Keys.FACE_DETECTION, enabled)

    /** @param enabled Detect people and general objects. */
    suspend fun setObjectDetectionEnabled(enabled: Boolean) = put(Keys.OBJECT_DETECTION, enabled)

    /** @param enabled Detect weapons. */
    suspend fun setWeaponDetectionEnabled(enabled: Boolean) = put(Keys.WEAPON_DETECTION, enabled)

    /** @param enabled Alert on movement in a static scene. */
    suspend fun setMotionDetectionEnabled(enabled: Boolean) = put(Keys.MOTION_DETECTION, enabled)

    /** @param enabled Infer age, gender and emotion. */
    suspend fun setAttributeAnalysisEnabled(enabled: Boolean) = put(Keys.ATTRIBUTE_ANALYSIS, enabled)

    /** @param enabled Play the alarm sound. */
    suspend fun setAlertSoundEnabled(enabled: Boolean) = put(Keys.ALERT_SOUND, enabled)

    /** @param enabled Vibrate on alerts. */
    suspend fun setVibrationEnabled(enabled: Boolean) = put(Keys.VIBRATION, enabled)

    /** @param enabled Post system notifications. */
    suspend fun setPushNotificationsEnabled(enabled: Boolean) = put(Keys.PUSH_NOTIFICATIONS, enabled)

    /** @param resolution Capture resolution for the live camera. */
    suspend fun setCameraResolution(resolution: CameraResolution) =
        put(Keys.CAMERA_RESOLUTION, resolution.name)

    /** @param days Retention window for alerts and events. */
    suspend fun setDataRetentionDays(days: Int) = put(Keys.DATA_RETENTION_DAYS, days)

    /** @param enabled Use the dark theme. */
    suspend fun setDarkMode(enabled: Boolean) = put(Keys.DARK_MODE, enabled)

    private suspend fun <T> put(key: Preferences.Key<T>, value: T) {
        dataStore.edit { preferences -> preferences[key] = value }
    }

    /** Preference keys. Renaming one silently resets that setting, so they are declared once. */
    internal object Keys {
        val CONFIDENCE_THRESHOLD = floatPreferencesKey("confidence_threshold")
        val MATCH_MARGIN = floatPreferencesKey("match_margin")
        val VOTE_FRAMES = intPreferencesKey("vote_frames")
        val FACE_DETECTION = booleanPreferencesKey("face_detection_enabled")
        val OBJECT_DETECTION = booleanPreferencesKey("object_detection_enabled")
        val WEAPON_DETECTION = booleanPreferencesKey("weapon_detection_enabled")
        val MOTION_DETECTION = booleanPreferencesKey("motion_detection_enabled")
        val ATTRIBUTE_ANALYSIS = booleanPreferencesKey("attribute_analysis_enabled")
        val ALERT_SOUND = booleanPreferencesKey("alert_sound_enabled")
        val VIBRATION = booleanPreferencesKey("vibration_enabled")
        val PUSH_NOTIFICATIONS = booleanPreferencesKey("push_notifications_enabled")
        val CAMERA_RESOLUTION = stringPreferencesKey("camera_resolution")
        val DATA_RETENTION_DAYS = intPreferencesKey("data_retention_days")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
    }
}

/**
 * Projects stored preferences onto [AppSettings], defaulting every absent key.
 *
 * An unrecognised camera resolution name — from a downgrade, or a hand-edited
 * file — falls back to the default rather than throwing, because a preference
 * read must never be able to crash the app.
 */
private fun Preferences.toAppSettings(): AppSettings {
    val defaults = AppSettings()

    return AppSettings(
        confidenceThreshold = this[SettingsDataStore.Keys.CONFIDENCE_THRESHOLD]
            ?: defaults.confidenceThreshold,
        matchMargin = this[SettingsDataStore.Keys.MATCH_MARGIN] ?: defaults.matchMargin,
        voteFrames = this[SettingsDataStore.Keys.VOTE_FRAMES] ?: defaults.voteFrames,
        faceDetectionEnabled = this[SettingsDataStore.Keys.FACE_DETECTION]
            ?: defaults.faceDetectionEnabled,
        objectDetectionEnabled = this[SettingsDataStore.Keys.OBJECT_DETECTION]
            ?: defaults.objectDetectionEnabled,
        weaponDetectionEnabled = this[SettingsDataStore.Keys.WEAPON_DETECTION]
            ?: defaults.weaponDetectionEnabled,
        motionDetectionEnabled = this[SettingsDataStore.Keys.MOTION_DETECTION]
            ?: defaults.motionDetectionEnabled,
        attributeAnalysisEnabled = this[SettingsDataStore.Keys.ATTRIBUTE_ANALYSIS]
            ?: defaults.attributeAnalysisEnabled,
        alertSoundEnabled = this[SettingsDataStore.Keys.ALERT_SOUND] ?: defaults.alertSoundEnabled,
        vibrationEnabled = this[SettingsDataStore.Keys.VIBRATION] ?: defaults.vibrationEnabled,
        pushNotificationsEnabled = this[SettingsDataStore.Keys.PUSH_NOTIFICATIONS]
            ?: defaults.pushNotificationsEnabled,
        cameraResolution = this[SettingsDataStore.Keys.CAMERA_RESOLUTION]
            ?.let { stored -> CameraResolution.entries.firstOrNull { it.name == stored } }
            ?: defaults.cameraResolution,
        dataRetentionDays = this[SettingsDataStore.Keys.DATA_RETENTION_DAYS]
            ?: defaults.dataRetentionDays,
        darkMode = this[SettingsDataStore.Keys.DARK_MODE] ?: defaults.darkMode,
    )
}
