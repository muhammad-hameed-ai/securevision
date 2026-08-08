package com.securevision.core.domain.repository

import com.securevision.core.model.AppSettings
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

    /** @param enabled Whether an unrecognised face sounds the alarm. */
    suspend fun updateAlarmOnUnknownFace(enabled: Boolean)

    /** @param enabled Whether a weapon detection sounds the critical alarm. */
    suspend fun updateAlarmOnWeapon(enabled: Boolean)

    /** @param enabled Whether movement in a static scene raises an alert. */
    suspend fun updateMotionDetectionEnabled(enabled: Boolean)

    /** @param threshold Minimum cosine similarity for a face match, in `0f..1f`. */
    suspend fun updateFaceMatchThreshold(threshold: Float)

    /** @param margin Minimum lead the best match must hold over the runner-up. */
    suspend fun updateFaceMatchMargin(margin: Float)

    /** @param frames Consecutive agreeing frames required to commit an identity. */
    suspend fun updateVotingFrameCount(frames: Int)

    /** @param enabled Whether the live screen records video with overlays. */
    suspend fun updateRecordingEnabled(enabled: Boolean)

    /** @param enabled Master switch for system notifications. */
    suspend fun updateNotificationsEnabled(enabled: Boolean)

    /** @param days How long alerts and events are retained before pruning. */
    suspend fun updateRetentionDays(days: Int)

    /** @param useDarkTheme `null` follows the system setting. */
    suspend fun updateUseDarkTheme(useDarkTheme: Boolean?)
}
