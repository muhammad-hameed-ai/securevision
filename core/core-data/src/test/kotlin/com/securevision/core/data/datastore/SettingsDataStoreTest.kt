package com.securevision.core.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.securevision.core.model.AppSettings
import com.securevision.core.model.CameraResolution
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Exercises the preferences store against a real file.
 *
 * Runs on the JVM without Robolectric: Preferences DataStore needs a `File` and a
 * `CoroutineScope`, not an Android `Context`.
 */
class SettingsDataStoreTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `returns model defaults when nothing has been written`() = runTest {
        val settings = settingsDataStore(backgroundScope)

        val actual = settings.settingsFlow.first()

        assertEquals(AppSettings(), actual)
    }

    @Test
    fun `writes and reads back a float setting`() = runTest {
        val settings = settingsDataStore(backgroundScope)

        settings.setConfidenceThreshold(0.91f)

        assertEquals(0.91f, settings.settingsFlow.first().confidenceThreshold, TOLERANCE)
    }

    @Test
    fun `writes and reads back an int setting`() = runTest {
        val settings = settingsDataStore(backgroundScope)

        settings.setVoteFrames(7)

        assertEquals(7, settings.settingsFlow.first().voteFrames)
    }

    @Test
    fun `writes and reads back a boolean setting`() = runTest {
        val settings = settingsDataStore(backgroundScope)

        settings.setAttributeAnalysisEnabled(true)
        settings.setAlertSoundEnabled(false)

        val actual = settings.settingsFlow.first()
        assertTrue(actual.attributeAnalysisEnabled)
        assertFalse(actual.alertSoundEnabled)
    }

    @Test
    fun `writes and reads back an enum setting`() = runTest {
        val settings = settingsDataStore(backgroundScope)

        settings.setCameraResolution(CameraResolution.FHD_1080)

        assertEquals(CameraResolution.FHD_1080, settings.settingsFlow.first().cameraResolution)
    }

    @Test
    fun `writing one setting leaves the others at their defaults`() = runTest {
        val settings = settingsDataStore(backgroundScope)
        val defaults = AppSettings()

        settings.setMatchMargin(0.2f)

        val actual = settings.settingsFlow.first()
        assertEquals(0.2f, actual.matchMargin, TOLERANCE)
        assertEquals(defaults.confidenceThreshold, actual.confidenceThreshold, TOLERANCE)
        assertEquals(defaults.voteFrames, actual.voteFrames)
        assertEquals(defaults.dataRetentionDays, actual.dataRetentionDays)
        assertEquals(defaults.darkMode, actual.darkMode)
    }

    @Test
    fun `values persist across a new store over the same file`() = runTest {
        val file = File(temporaryFolder.root, STORE_FILE_NAME)

        // DataStore permits exactly one active instance per file and throws
        // otherwise. Cancelling the first scope and waiting for it to finish is
        // what releases the file — and it models a process restart far more
        // honestly than opening a second store alongside the first would.
        val firstStoreJob = Job()
        settingsDataStore(CoroutineScope(coroutineContext + firstStoreJob), file)
            .setDataRetentionDays(90)
        firstStoreJob.cancelAndJoin()

        val reopened = settingsDataStore(backgroundScope, file)

        assertEquals(90, reopened.settingsFlow.first().dataRetentionDays)
    }

    private fun settingsDataStore(
        scope: CoroutineScope,
        file: File = File(temporaryFolder.root, STORE_FILE_NAME),
    ): SettingsDataStore {
        val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { file },
        )

        return SettingsDataStore(dataStore)
    }

    private companion object {
        const val STORE_FILE_NAME = "settings.preferences_pb"
        const val TOLERANCE = 1e-6f
    }
}
