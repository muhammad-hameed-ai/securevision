package com.securevision.core.domain.repository

import com.securevision.core.model.AppSettings
import com.securevision.core.model.CameraResolution
import kotlinx.coroutines.flow.Flow

/**
 * User preferences, persisted on-device.
 *
 * Exposes one stream of the whole [AppSettings] object plus a narrow setter per
 * field. Field-level setters rather than a single `update(AppSettings)` so that
 * two screens writing different settings concurrently cannot overwrite each
 * other's change.
 */
interface SettingsRepository {

    /** Current settings, re-emitted on every change. */
    val settingsFlow: Flow<AppSettings>

    // --- Recognition tuning --------------------------------------------------

    /** @param threshold Minimum cosine similarity for a face match, in `0f..1f`. */
    suspend fun updateConfidenceThreshold(threshold: Float)

    /** @param margin Minimum lead the best match must hold over the runner-up. */
    suspend fun updateMatchMargin(margin: Float)

    /** @param frames Consecutive agreeing frames required to commit an identity. */
    suspend fun updateVoteFrames(frames: Int)

    // --- Detector switches ---------------------------------------------------

    /** @param enabled Detect and recognise faces. */
    suspend fun updateFaceDetectionEnabled(enabled: Boolean)

    /** @param enabled Detect people and general objects. */
    suspend fun updateObjectDetectionEnabled(enabled: Boolean)

    /** @param enabled Detect weapons and raise a critical alarm. */
    suspend fun updateWeaponDetectionEnabled(enabled: Boolean)

    /** @param enabled Alert on movement in a static scene. */
    suspend fun updateMotionDetectionEnabled(enabled: Boolean)

    /** @param enabled Infer age, gender and emotion in addition to beard and mask. */
    suspend fun updateAttributeAnalysisEnabled(enabled: Boolean)

    // --- Alerting ------------------------------------------------------------

    /** @param enabled Play the alarm sound on qualifying alerts. */
    suspend fun updateAlertSoundEnabled(enabled: Boolean)

    /** @param enabled Vibrate on qualifying alerts. */
    suspend fun updateVibrationEnabled(enabled: Boolean)

    /** @param enabled Post system notifications. */
    suspend fun updatePushNotificationsEnabled(enabled: Boolean)

    // --- Capture and housekeeping -------------------------------------------

    /** @param resolution Capture resolution for the live camera. */
    suspend fun updateCameraResolution(resolution: CameraResolution)

    /** @param days How long alerts and events are retained before pruning. */
    suspend fun updateDataRetentionDays(days: Int)

    /** @param enabled Use the dark theme. */
    suspend fun updateDarkMode(enabled: Boolean)
}
