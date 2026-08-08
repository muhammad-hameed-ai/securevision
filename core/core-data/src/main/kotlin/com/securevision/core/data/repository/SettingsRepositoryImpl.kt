package com.securevision.core.data.repository

import com.securevision.core.data.datastore.SettingsDataStore
import com.securevision.core.domain.repository.SettingsRepository
import com.securevision.core.model.AppSettings
import com.securevision.core.model.CameraResolution
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/**
 * DataStore-backed user preferences.
 *
 * A thin pass-through: [SettingsDataStore] already runs off the main thread and
 * defaults absent keys, so adding another dispatcher hop here would buy nothing.
 *
 * @property settingsDataStore The underlying preferences store.
 */
@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
) : SettingsRepository {

    override val settingsFlow: Flow<AppSettings> = settingsDataStore.settingsFlow

    override suspend fun updateConfidenceThreshold(threshold: Float) =
        settingsDataStore.setConfidenceThreshold(threshold)

    override suspend fun updateMatchMargin(margin: Float) =
        settingsDataStore.setMatchMargin(margin)

    override suspend fun updateVoteFrames(frames: Int) =
        settingsDataStore.setVoteFrames(frames)

    override suspend fun updateFaceDetectionEnabled(enabled: Boolean) =
        settingsDataStore.setFaceDetectionEnabled(enabled)

    override suspend fun updateObjectDetectionEnabled(enabled: Boolean) =
        settingsDataStore.setObjectDetectionEnabled(enabled)

    override suspend fun updateWeaponDetectionEnabled(enabled: Boolean) =
        settingsDataStore.setWeaponDetectionEnabled(enabled)

    override suspend fun updateMotionDetectionEnabled(enabled: Boolean) =
        settingsDataStore.setMotionDetectionEnabled(enabled)

    override suspend fun updateAttributeAnalysisEnabled(enabled: Boolean) =
        settingsDataStore.setAttributeAnalysisEnabled(enabled)

    override suspend fun updateAlertSoundEnabled(enabled: Boolean) =
        settingsDataStore.setAlertSoundEnabled(enabled)

    override suspend fun updateVibrationEnabled(enabled: Boolean) =
        settingsDataStore.setVibrationEnabled(enabled)

    override suspend fun updatePushNotificationsEnabled(enabled: Boolean) =
        settingsDataStore.setPushNotificationsEnabled(enabled)

    override suspend fun updateCameraResolution(resolution: CameraResolution) =
        settingsDataStore.setCameraResolution(resolution)

    override suspend fun updateDataRetentionDays(days: Int) =
        settingsDataStore.setDataRetentionDays(days)

    override suspend fun updateDarkMode(enabled: Boolean) =
        settingsDataStore.setDarkMode(enabled)
}
