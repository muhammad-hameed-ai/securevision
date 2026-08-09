package com.securevision.feature.live

import com.securevision.core.common.dispatcher.DispatcherProvider
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
import com.securevision.core.domain.repository.SettingsRepository
import com.securevision.core.domain.usecase.dashboard.GetEnrolledProfileCountUseCase
import com.securevision.core.domain.usecase.live.AnalyseAttributesUseCase
import com.securevision.core.domain.usecase.live.CaptureSnapshotUseCase
import com.securevision.core.domain.usecase.live.DetectMotionUseCase
import com.securevision.core.domain.usecase.live.DetectWeaponsUseCase
import com.securevision.core.domain.usecase.live.EnrolFaceFromFrameUseCase
import com.securevision.core.domain.usecase.live.PrepareDetectorsUseCase
import com.securevision.core.domain.usecase.live.RecogniseFacesUseCase
import com.securevision.core.domain.usecase.live.RecordMotionSightingUseCase
import com.securevision.core.domain.usecase.live.RecordUnknownSightingUseCase
import com.securevision.core.domain.usecase.live.RecordWeaponSightingUseCase
import com.securevision.core.domain.usecase.profile.GetEnrolledProfilesUseCase
import com.securevision.core.domain.usecase.settings.ObserveSettingsUseCase
import com.securevision.core.model.AlertRecord
import com.securevision.core.model.AppSettings
import com.securevision.core.model.AttributeAvailability
import com.securevision.core.model.BoundingBox
import com.securevision.core.model.DetectionResult
import com.securevision.core.model.MatchStatus
import com.securevision.core.model.MotionResult
import com.securevision.core.model.WeaponDetection
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
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
    private val alertRepository = mockk<AlertRepository>(relaxed = true)
    private val eventRepository = mockk<DetectionEventRepository>(relaxed = true)
    private val profileRepository = mockk<EnrolledProfileRepository>(relaxed = true)
    private val settingsRepository = mockk<SettingsRepository>(relaxed = true)
    private val photoStore = mockk<ProfilePhotoStore>(relaxed = true)
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
    fun `a recognised person raises no alert`() = runTest {
        stubFaces(face(trackingId = 7, status = MatchStatus.KNOWN, profileId = "ayesha"))
        val viewModel = viewModel()

        viewModel.onFrame(frameBitmap, isFrontCamera = false)

        coVerify(exactly = 0) { alertRepository.save(any()) }
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
    fun `enrolling with no captured frame reports no face rather than crashing`() = runTest {
        val viewModel = viewModel()

        viewModel.enrolCurrentFace(name = "Ayesha", age = 30)

        assertEquals(
            EnrolmentEvent.Failed(EnrolmentCapture.Failure.Reason.NO_FACE_DETECTED),
            viewModel.enrolmentEvent.value,
        )
    }

    @Test
    fun `consuming the enrolment event clears it`() = runTest {
        val viewModel = viewModel()
        viewModel.enrolCurrentFace(name = "Ayesha", age = 30)

        viewModel.consumeEnrolmentEvent()

        assertEquals(null, viewModel.enrolmentEvent.value)
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
        enrolFaceFromFrame = EnrolFaceFromFrameUseCase(
            engine = faceEngine,
            photoStore = photoStore,
            repository = profileRepository,
            dispatcherProvider = dispatchers,
        ),
        recordUnknownSighting = RecordUnknownSightingUseCase(
            alertRepository = alertRepository,
            detectionEventRepository = eventRepository,
            dispatcherProvider = dispatchers,
        ),
        recordWeaponSighting = RecordWeaponSightingUseCase(
            alertRepository = alertRepository,
            detectionEventRepository = eventRepository,
            dispatcherProvider = dispatchers,
        ),
        recordMotionSighting = RecordMotionSightingUseCase(
            alertRepository = alertRepository,
            detectionEventRepository = eventRepository,
            dispatcherProvider = dispatchers,
        ),
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
    }
}
