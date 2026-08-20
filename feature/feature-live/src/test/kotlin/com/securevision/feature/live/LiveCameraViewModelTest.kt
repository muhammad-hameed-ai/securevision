package com.securevision.feature.live

import com.securevision.core.common.dispatcher.DispatcherProvider
import com.securevision.core.domain.alerting.AlarmPlayer
import com.securevision.core.domain.alerting.AlertGate
import com.securevision.core.domain.alerting.AlertNotifier
import com.securevision.core.domain.alerting.NotificationOutcome
import com.securevision.core.domain.engine.AttributeAnalysisEngine
import com.securevision.core.domain.engine.EngineStatus
import com.securevision.core.domain.engine.EnrolmentCapture
import com.securevision.core.domain.engine.FaceRecognitionEngine
import com.securevision.core.domain.engine.InferenceDelegate
import com.securevision.core.domain.engine.MotionDetectionEngine
import com.securevision.core.domain.engine.ProfilePhotoStore
import com.securevision.core.domain.engine.RecognisedFace
import com.securevision.core.domain.engine.SnapshotStore
import com.securevision.core.domain.engine.WeaponDetectionEngine
import com.securevision.core.domain.repository.AlertRepository
import com.securevision.core.domain.repository.DetectionEventRepository
import com.securevision.core.domain.repository.EnrolledProfileRepository
import com.securevision.core.domain.repository.RecordingRepository
import com.securevision.core.domain.repository.SettingsRepository
import com.securevision.core.domain.usecase.dashboard.GetEnrolledProfileCountUseCase
import com.securevision.core.domain.usecase.live.AnalyseAttributesUseCase
import com.securevision.core.domain.usecase.live.CaptureSnapshotUseCase
import com.securevision.core.domain.usecase.live.DetectMotionUseCase
import com.securevision.core.domain.usecase.live.DetectWeaponsUseCase
import com.securevision.core.domain.usecase.live.PrepareDetectorsUseCase
import com.securevision.core.domain.usecase.live.RaiseAlertUseCase
import com.securevision.core.domain.usecase.live.RecogniseFacesUseCase
import com.securevision.core.domain.usecase.live.SilenceAlarmUseCase
import com.securevision.core.domain.usecase.live.SoundAlarmUseCase
import com.securevision.core.domain.usecase.profile.GetEnrolledProfilesUseCase
import com.securevision.core.domain.usecase.recording.SaveRecordingUseCase
import com.securevision.core.domain.usecase.settings.ObserveSettingsUseCase
import com.securevision.core.model.AlertRecord
import com.securevision.core.model.AlertType
import com.securevision.core.model.Severity
import com.securevision.core.model.AppSettings
import com.securevision.core.model.AttributeAvailability
import com.securevision.core.model.BoundingBox
import com.securevision.core.model.DetectionResult
import com.securevision.core.model.MatchStatus
import com.securevision.core.model.MotionResult
import com.securevision.core.model.Recording
import com.securevision.core.model.WeaponDetection
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Session accounting, detector dispatch and alert de-duplication.
 *
 * Bitmaps cannot be constructed in a JVM unit test, so the frame is a mock and
 * every engine is stubbed. What is covered here is the ViewModel's own logic —
 * counting per tracking id rather than per frame, not writing thirty alerts for
 * one person standing still, and not letting one detector's alert suppress
 * another's.
 */
class LiveCameraViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val dispatchers = object : DispatcherProvider {
        override val io: CoroutineDispatcher = testDispatcher
        override val default: CoroutineDispatcher = testDispatcher
        override val main: CoroutineDispatcher = testDispatcher
    }

    private val faceEngine = mockk<FaceRecognitionEngine>(relaxed = true)
    private val weaponEngine = mockk<WeaponDetectionEngine>(relaxed = true)
    private val motionEngine = mockk<MotionDetectionEngine>(relaxed = true)
    private val attributeEngine = mockk<AttributeAnalysisEngine>(relaxed = true)
    private val snapshotStore = mockk<SnapshotStore>(relaxed = true)
    private val alarmPlayer = mockk<AlarmPlayer>(relaxed = true)
    private val notifier = mockk<AlertNotifier>(relaxed = true)

    /** The real gate, not a mock: its behaviour is what these tests exercise. */
    private val gate = AlertGate()
    private val alertRepository = mockk<AlertRepository>(relaxed = true)
    private val eventRepository = mockk<DetectionEventRepository>(relaxed = true)
    private val profileRepository = mockk<EnrolledProfileRepository>(relaxed = true)
    private val settingsRepository = mockk<SettingsRepository>(relaxed = true)
    private val photoStore = mockk<ProfilePhotoStore>(relaxed = true)
    private val recordingRepository = mockk<RecordingRepository>(relaxed = true)
    private val frameBitmap = mockk<android.graphics.Bitmap>(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        every { frameBitmap.width } returns 480
        every { frameBitmap.height } returns 640
        every { profileRepository.getAll() } returns flowOf(emptyList())
        every { profileRepository.countAll() } returns flowOf(0)
        every { settingsRepository.settingsFlow } returns flowOf(AppSettings())

        coEvery { faceEngine.prepare(any()) } returns READY_512
        coEvery { faceEngine.recognise(any(), any(), any(), any()) } returns emptyList()
        coEvery { weaponEngine.prepare() } returns READY_512
        coEvery { weaponEngine.detect(any(), any()) } returns emptyList()
        coEvery { motionEngine.detect(any(), any()) } returns MotionResult.NONE
        coEvery { attributeEngine.prepare() } returns AttributeAvailability()
        coEvery { notifier.post(any()) } returns NotificationOutcome.POSTED
        every { alarmPlayer.isSounding } returns false
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `starts ready with empty stats`() = runTest {
        val state = viewModel().uiState.value

        assertTrue(state is LiveUiState.Ready)
        assertEquals(SessionStats(), (state as LiveUiState.Ready).stats)
    }

    @Test
    fun `reports each detector status separately once the models have loaded`() = runTest {
        val state = viewModel().uiState.value as LiveUiState.Ready

        assertTrue(state.isRecognitionActive)
        assertTrue(state.isWeaponDetectionActive)
        assertEquals(READY_512, state.faceEngineStatus)
    }

    @Test
    fun `surfaces a missing face model instead of pretending recognition works`() = runTest {
        coEvery { faceEngine.prepare(any()) } returns unavailable()

        val state = viewModel().uiState.value as LiveUiState.Ready

        assertFalse(state.isRecognitionActive)
    }

    @Test
    fun `a missing weapon model does not disable face recognition`() = runTest {
        // The two fail independently; one combined status would take both down.
        coEvery { weaponEngine.prepare() } returns unavailable()

        val state = viewModel().uiState.value as LiveUiState.Ready

        assertFalse(state.isWeaponDetectionActive)
        assertTrue(state.isRecognitionActive)
    }

    @Test
    fun `counts one sighting per tracking id, not per frame`() = runTest {
        stubFaces(face(trackingId = 7, status = MatchStatus.UNKNOWN))
        val viewModel = viewModel()

        repeat(5) { viewModel.onFrame(frameBitmap, isFrontCamera = false) }

        val stats = (viewModel.uiState.value as LiveUiState.Ready).stats
        assertEquals(1, stats.total)
        assertEquals(1, stats.unknown)
        assertEquals(0, stats.known)
    }

    @Test
    fun `a face that becomes known stops being counted as unknown`() = runTest {
        val viewModel = viewModel()

        stubFaces(face(trackingId = 7, status = MatchStatus.UNKNOWN))
        viewModel.onFrame(frameBitmap, isFrontCamera = false)

        stubFaces(face(trackingId = 7, status = MatchStatus.KNOWN, profileId = "ayesha"))
        advanceThrottle(viewModel)

        val stats = (viewModel.uiState.value as LiveUiState.Ready).stats
        assertEquals(1, stats.total)
        assertEquals(1, stats.known)
        assertEquals(0, stats.unknown)
    }

    @Test
    fun `an unresolved face is counted as seen but neither known nor unknown`() = runTest {
        stubFaces(face(trackingId = 3, status = MatchStatus.PROCESSING))
        val viewModel = viewModel()

        viewModel.onFrame(frameBitmap, isFrontCamera = false)

        val stats = (viewModel.uiState.value as LiveUiState.Ready).stats
        assertEquals(1, stats.total)
        assertEquals(0, stats.known)
        assertEquals(0, stats.unknown)
    }

    @Test
    fun `writes one alert per stranger, not one per frame`() = runTest {
        stubFaces(face(trackingId = 7, status = MatchStatus.UNKNOWN))
        val viewModel = viewModel()

        repeat(10) { viewModel.onFrame(frameBitmap, isFrontCamera = false) }

        coVerify(exactly = 1) { alertRepository.save(any()) }
        coVerify(exactly = 1) { eventRepository.save(any()) }
    }

    @Test
    fun `a recognised person raises no stranger alert`() = runTest {
        val saved = mutableListOf<AlertRecord>()
        coEvery { alertRepository.save(capture(saved)) } returns Unit

        stubFaces(face(trackingId = 7, status = MatchStatus.KNOWN, profileId = "ayesha"))
        val viewModel = viewModel()

        viewModel.onFrame(frameBitmap, isFrontCamera = false)

        // Recognition produces a KNOWN entry, never an UNKNOWN one. Phase 7
        // changed this from "no alert at all" so a recognition is visible.
        assertEquals(0, saved.count { it.type == AlertType.UNKNOWN_PERSON })
        assertEquals(1, saved.count { it.type == AlertType.KNOWN_PERSON })
    }

    @Test
    fun `a weapon alert is not swallowed by a face alert in the same frame`() = runTest {
        // The de-duplication window is per alert kind. Were it global, the stranger
        // alert would suppress the weapon alert beside it — the most severe event
        // in the app, silently dropped because of an unrelated one.
        stubFaces(face(trackingId = 7, status = MatchStatus.UNKNOWN))
        stubWeapons(weapon(type = "pistol"))
        val viewModel = viewModel()

        viewModel.onFrame(frameBitmap, isFrontCamera = false)

        // Two alerts from one frame: the stranger and the weapon.
        coVerify(exactly = 2) { alertRepository.save(any()) }
        assertEquals(1, (viewModel.uiState.value as LiveUiState.Ready).stats.weapons)
    }

    @Test
    fun `writes one alert per weapon class, not one per frame`() = runTest {
        stubWeapons(weapon(type = "pistol"))
        val viewModel = viewModel()

        repeat(10) { viewModel.onFrame(frameBitmap, isFrontCamera = false) }

        coVerify(exactly = 1) { alertRepository.save(any()) }
        assertEquals(1, (viewModel.uiState.value as LiveUiState.Ready).stats.weapons)
    }

    @Test
    fun `weapon detection is skipped when the setting is off`() = runTest {
        every { settingsRepository.settingsFlow } returns
            flowOf(AppSettings(weaponDetectionEnabled = false))
        stubWeapons(weapon(type = "pistol"))
        val viewModel = viewModel()

        viewModel.onFrame(frameBitmap, isFrontCamera = false)

        coVerify(exactly = 0) { weaponEngine.detect(any(), any()) }
        assertTrue((viewModel.uiState.value as LiveUiState.Ready).weapons.isEmpty())
    }

    @Test
    fun `a new motion event is recorded once`() = runTest {
        coEvery { motionEngine.detect(any(), any()) } returns
            MotionResult(hasMotion = true, intensity = 0.4f, isNewEvent = true)
        val viewModel = viewModel()

        repeat(4) { viewModel.onFrame(frameBitmap, isFrontCamera = false) }

        coVerify(exactly = 1) { alertRepository.save(any()) }
    }

    @Test
    fun `continuing motion that is not a new event raises no alert`() = runTest {
        coEvery { motionEngine.detect(any(), any()) } returns
            MotionResult(hasMotion = true, intensity = 0.4f, isNewEvent = false)
        val viewModel = viewModel()

        viewModel.onFrame(frameBitmap, isFrontCamera = false)

        coVerify(exactly = 0) { alertRepository.save(any()) }
        assertTrue((viewModel.uiState.value as LiveUiState.Ready).motion.hasMotion)
    }

    @Test
    fun `attributes stay unassessed when analysis is switched off`() = runTest {
        // Null means "not assessed" and must never be written as false.
        stubFaces(face(trackingId = 7, status = MatchStatus.UNKNOWN))
        val viewModel = viewModel()

        viewModel.onFrame(frameBitmap, isFrontCamera = false)

        val saved = slot<AlertRecord>()
        coVerify { alertRepository.save(capture(saved)) }
        assertNull(saved.captured.hasBeard)
        assertNull(saved.captured.hasMask)
        coVerify(exactly = 0) { attributeEngine.analyse(any(), any()) }
    }

    @Test
    fun `flipping the camera resets the session counters and the motion baseline`() = runTest {
        stubFaces(face(trackingId = 7, status = MatchStatus.UNKNOWN))
        val viewModel = viewModel()
        viewModel.onFrame(frameBitmap, isFrontCamera = false)

        viewModel.flipCamera()

        // Tracking ids restart on the other lens, so carrying counts across would
        // double-count the same person, and the lens change itself reads as motion.
        val state = viewModel.uiState.value as LiveUiState.Ready
        assertEquals(SessionStats(), state.stats)
        assertTrue(state.isFrontCamera)
        assertTrue(state.detections.isEmpty())
        assertTrue(state.weapons.isEmpty())
        assertEquals(MotionResult.NONE, state.motion)
        coVerify { motionEngine.reset() }
    }

    @Test
    fun `a critical alert offers the silence control`() = runTest {
        every { alarmPlayer.isSounding } returns true
        stubWeapons(weapon(type = "pistol"))
        val viewModel = viewModel()

        viewModel.onFrame(frameBitmap, isFrontCamera = false)

        assertTrue((viewModel.uiState.value as LiveUiState.Ready).isAlarmSounding)
    }

    @Test
    fun `silencing stops the tone without touching the record`() = runTest {
        every { alarmPlayer.isSounding } returns true
        stubWeapons(weapon(type = "pistol"))
        val viewModel = viewModel()
        viewModel.onFrame(frameBitmap, isFrontCamera = false)

        viewModel.silenceAlarm()

        verify(exactly = 1) { alarmPlayer.silence() }
        assertFalse((viewModel.uiState.value as LiveUiState.Ready).isAlarmSounding)
        // Silencing means "I have heard it", not "that did not happen".
        coVerify(exactly = 1) { alertRepository.save(any()) }
    }

    @Test
    fun `a refused notification permission is explained, not hidden`() = runTest {
        coEvery { notifier.post(any()) } returns NotificationOutcome.PERMISSION_DENIED
        stubFaces(face(trackingId = 7, status = MatchStatus.UNKNOWN))
        val viewModel = viewModel()

        viewModel.onFrame(frameBitmap, isFrontCamera = false)

        val state = viewModel.uiState.value as LiveUiState.Ready
        assertTrue(state.notificationsBlocked)
        // The alert was still recorded; only the shade missed out.
        coVerify(exactly = 1) { alertRepository.save(any()) }
    }

    @Test
    fun `enrolling mid-session stops the same face reading as unknown`() = runTest {
        // The reported symptom was unknown alerts for an enrolled person. The
        // profile list is a Flow and the voter is a sliding window, so a face
        // seen before enrolment resolves to KNOWN once the profile lands — with
        // no restart and nothing to invalidate.
        val saved = mutableListOf<AlertRecord>()
        coEvery { alertRepository.save(capture(saved)) } returns Unit

        stubFaces(face(trackingId = 7, status = MatchStatus.UNKNOWN))
        val viewModel = viewModel()
        viewModel.onFrame(frameBitmap, isFrontCamera = false)

        // The person is now enrolled and the matcher recognises them.
        stubFaces(face(trackingId = 7, status = MatchStatus.KNOWN, profileId = "ayesha"))
        advanceThrottle(viewModel)

        val state = viewModel.uiState.value as LiveUiState.Ready
        assertEquals(MatchStatus.KNOWN, state.detections.first().matchStatus)

        // Exactly one UNKNOWN — the original, historical one. The recognition
        // that follows adds a KNOWN entry, never a second stranger alert.
        assertEquals(1, saved.count { it.type == AlertType.UNKNOWN_PERSON })
        assertEquals(1, saved.count { it.type == AlertType.KNOWN_PERSON })
    }

    @Test
    fun `a recognised person is visible but silent`() = runTest {
        stubFaces(face(trackingId = 7, status = MatchStatus.KNOWN, profileId = "ayesha"))
        val viewModel = viewModel()

        viewModel.onFrame(frameBitmap, isFrontCamera = false)

        val saved = slot<AlertRecord>()
        coVerify(exactly = 1) { alertRepository.save(capture(saved)) }

        // Visible in the alerts list, and named — an unnamed "recognised person"
        // entry would say nothing worth recording.
        assertEquals(AlertType.KNOWN_PERSON, saved.captured.type)
        assertEquals(Severity.LOW, saved.captured.severity)
        assertEquals("ayesha", saved.captured.label)

        // But never announced: no notification, and LOW produces no tone.
        coVerify(exactly = 0) { notifier.post(any()) }
        coVerify(exactly = 1) { eventRepository.save(any()) }
    }

    @Test
    fun `flipping the camera clears vote history`() = runTest {
        val viewModel = viewModel()

        viewModel.flipCamera()

        // Tracking ids restart on the new lens and are commonly reused, so stale
        // votes would put the previous person's name on a different face.
        coVerify(exactly = 1) { faceEngine.resetTracking() }
    }

    @Test
    fun `a stalled analysis pass cannot eat the next flip`() = runTest {
        val viewModel = viewModel()

        // First flip leaves the switching state set and, in the field, could
        // leave the analysis latch stuck if a pass never completed.
        viewModel.flipCamera()
        assertTrue((viewModel.uiState.value as LiveUiState.Ready).isSwitchingCamera)

        // Past the debounce, a second tap must still be honoured — the previous
        // build rejected it for as long as the switching flag was set, which is
        // what made the button need four or five presses.
        Thread.sleep(FLIP_DEBOUNCE_WAIT_MILLIS)
        viewModel.flipCamera()

        assertFalse((viewModel.uiState.value as LiveUiState.Ready).isFrontCamera)
    }

    @Test
    fun `the bind result ends the switch without waiting for a frame`() = runTest {
        val viewModel = viewModel()
        viewModel.flipCamera()

        viewModel.onCameraBound("FULL")

        // No frame required. Tying this to analysis is what left the spinner and
        // the disabled control outliving the actual switch.
        assertFalse((viewModel.uiState.value as LiveUiState.Ready).isSwitchingCamera)
    }

    @Test
    fun `flipping shows a switching state until a frame arrives`() = runTest {
        val viewModel = viewModel()

        viewModel.flipCamera()

        // The UI responds on tap rather than after the rebind, which is what
        // made the control feel dead.
        assertTrue((viewModel.uiState.value as LiveUiState.Ready).isSwitchingCamera)

        viewModel.onFrame(frameBitmap, isFrontCamera = true)

        assertFalse((viewModel.uiState.value as LiveUiState.Ready).isSwitchingCamera)
    }

    @Test
    fun `recording state reaches the screen`() = runTest {
        val viewModel = viewModel()

        viewModel.onRecordingStateChange(isRecording = true, elapsedMillis = 4_000L)

        val state = viewModel.uiState.value as LiveUiState.Ready
        assertTrue(state.isRecording)
        assertEquals(4_000L, state.recordingElapsedMillis)
    }

    @Test
    fun `a finished clip is saved`() = runTest {
        val viewModel = viewModel()

        viewModel.onRecordingFinished(
            Recording(
                id = "clip-1",
                filePath = "/data/recordings/clip-1.mp4",
                durationMs = 5_000L,
                thumbnailUri = null,
                createdAt = 1_700_000_000_000L,
            ),
        )

        coVerify(exactly = 1) { recordingRepository.save(any()) }
    }

    /** Waits past the analysis throttle so the next frame is not dropped. */
    private fun advanceThrottle(viewModel: LiveCameraViewModel) {
        Thread.sleep(THROTTLE_MILLIS)
        viewModel.onFrame(frameBitmap, isFrontCamera = false)
    }

    private fun stubFaces(vararg faces: RecognisedFace) {
        coEvery { faceEngine.recognise(any(), any(), any(), any()) } returns faces.toList()
    }

    private fun stubWeapons(vararg weapons: WeaponDetection) {
        coEvery { weaponEngine.detect(any(), any()) } returns weapons.toList()
    }

    private fun viewModel() = LiveCameraViewModel(
        prepareDetectors = PrepareDetectorsUseCase(
            faceEngine = faceEngine,
            weaponEngine = weaponEngine,
            attributeEngine = attributeEngine,
            motionEngine = motionEngine,
            dispatcherProvider = dispatchers,
        ),
        recogniseFaces = RecogniseFacesUseCase(faceEngine, dispatchers),
        detectWeapons = DetectWeaponsUseCase(weaponEngine, dispatchers),
        detectMotion = DetectMotionUseCase(motionEngine, dispatchers),
        analyseAttributes = AnalyseAttributesUseCase(attributeEngine, dispatchers),
        captureSnapshot = CaptureSnapshotUseCase(snapshotStore),
        saveRecording = SaveRecordingUseCase(recordingRepository, dispatchers),
        raiseAlert = RaiseAlertUseCase(
            gate = gate,
            alertRepository = alertRepository,
            detectionEventRepository = eventRepository,
            settingsRepository = settingsRepository,
            alarmPlayer = alarmPlayer,
            notifier = notifier,
            dispatcherProvider = dispatchers,
        ),
        silenceAlarm = SilenceAlarmUseCase(alarmPlayer),
        soundAlarm = SoundAlarmUseCase(alarmPlayer, settingsRepository),
        alertGate = gate,
        getEnrolledProfiles = GetEnrolledProfilesUseCase(profileRepository, dispatchers),
        getEnrolledProfileCount = GetEnrolledProfileCountUseCase(profileRepository, dispatchers),
        observeSettings = ObserveSettingsUseCase(settingsRepository, dispatchers),
    )

    private fun face(
        trackingId: Int,
        status: MatchStatus,
        profileId: String? = null,
    ) = RecognisedFace(
        detection = DetectionResult(
            trackingId = trackingId,
            boundingBox = BoundingBox(0.3f, 0.3f, 0.6f, 0.7f),
            matchStatus = status,
            profileId = profileId,
            profileName = profileId,
            confidence = 0.82f,
        ),
    )

    private fun weapon(type: String) = WeaponDetection(
        weaponType = type,
        confidence = 0.91f,
        boundingBox = BoundingBox(0.1f, 0.1f, 0.4f, 0.5f),
    )

    private fun unavailable() = EngineStatus.RecognitionUnavailable(
        EngineStatus.RecognitionUnavailable.Reason.MODEL_NOT_INSTALLED,
    )

    private companion object {
        val READY_512 = EngineStatus.Ready(
            embeddingDimensions = 512,
            delegate = InferenceDelegate.CPU,
        )

        /** Matches the ViewModel's analysis interval, plus a margin. */
        const val THROTTLE_MILLIS = 400L

        /** Just past the flip debounce, so a deliberate second tap is accepted. */
        const val FLIP_DEBOUNCE_WAIT_MILLIS = 500L
    }
}
