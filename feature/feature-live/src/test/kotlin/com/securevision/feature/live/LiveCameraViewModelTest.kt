package com.securevision.feature.live

import com.securevision.core.common.dispatcher.DispatcherProvider
import com.securevision.core.domain.engine.EngineStatus
import com.securevision.core.domain.engine.EnrolmentCapture
import com.securevision.core.domain.engine.FaceFrame
import com.securevision.core.domain.engine.FaceRecognitionEngine
import com.securevision.core.domain.engine.InferenceDelegate
import com.securevision.core.domain.engine.ProfilePhotoStore
import com.securevision.core.domain.repository.AlertRepository
import com.securevision.core.domain.repository.DetectionEventRepository
import com.securevision.core.domain.repository.EnrolledProfileRepository
import com.securevision.core.domain.repository.SettingsRepository
import com.securevision.core.domain.usecase.dashboard.GetEnrolledProfileCountUseCase
import com.securevision.core.domain.usecase.live.EnrolFaceFromFrameUseCase
import com.securevision.core.domain.usecase.live.PrepareRecognitionUseCase
import com.securevision.core.domain.usecase.live.RecogniseFacesUseCase
import com.securevision.core.domain.usecase.live.RecordUnknownSightingUseCase
import com.securevision.core.domain.usecase.profile.GetEnrolledProfilesUseCase
import com.securevision.core.domain.usecase.settings.ObserveSettingsUseCase
import com.securevision.core.model.AppSettings
import com.securevision.core.model.BoundingBox
import com.securevision.core.model.DetectionResult
import com.securevision.core.model.MatchStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Session accounting and alert de-duplication.
 *
 * Bitmaps cannot be constructed in a JVM unit test, so the frame is a mock and
 * the engine is stubbed. What is covered here is the ViewModel's own logic —
 * counting per tracking id rather than per frame, and not writing thirty alerts
 * for one person standing still.
 */
class LiveCameraViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val dispatchers = object : DispatcherProvider {
        override val io: CoroutineDispatcher = testDispatcher
        override val default: CoroutineDispatcher = testDispatcher
        override val main: CoroutineDispatcher = testDispatcher
    }

    private val engine = mockk<FaceRecognitionEngine>(relaxed = true)
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
        coEvery { engine.prepare(any()) } returns EngineStatus.Ready(
            embeddingDimensions = 512,
            delegate = InferenceDelegate.CPU,
        )
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
    fun `reports the engine status once the model has loaded`() = runTest {
        val state = viewModel().uiState.value as LiveUiState.Ready

        assertTrue(state.isRecognitionActive)
        assertEquals(
            EngineStatus.Ready(embeddingDimensions = 512, delegate = InferenceDelegate.CPU),
            state.engineStatus,
        )
    }

    @Test
    fun `surfaces a missing model instead of pretending recognition works`() = runTest {
        coEvery { engine.prepare(any()) } returns EngineStatus.RecognitionUnavailable(
            EngineStatus.RecognitionUnavailable.Reason.MODEL_NOT_INSTALLED,
        )

        val state = viewModel().uiState.value as LiveUiState.Ready

        assertFalse(state.isRecognitionActive)
    }

    @Test
    fun `counts one sighting per tracking id, not per frame`() = runTest {
        coEvery { engine.recognise(any(), any(), any()) } returns
            listOf(detection(trackingId = 7, status = MatchStatus.UNKNOWN))
        val viewModel = viewModel()

        // Same face over many frames. Throttling means only the first is analysed
        // in this window, but even several would still be one sighting.
        repeat(5) { viewModel.onFrame(frameBitmap, isFrontCamera = false) }

        val stats = (viewModel.uiState.value as LiveUiState.Ready).stats
        assertEquals(1, stats.total)
        assertEquals(1, stats.unknown)
        assertEquals(0, stats.known)
    }

    @Test
    fun `a face that becomes known stops being counted as unknown`() = runTest {
        val viewModel = viewModel()

        coEvery { engine.recognise(any(), any(), any()) } returns
            listOf(detection(trackingId = 7, status = MatchStatus.UNKNOWN))
        viewModel.onFrame(frameBitmap, isFrontCamera = false)

        coEvery { engine.recognise(any(), any(), any()) } returns
            listOf(detection(trackingId = 7, status = MatchStatus.KNOWN, profileId = "ayesha"))
        advanceThrottle(viewModel)

        val stats = (viewModel.uiState.value as LiveUiState.Ready).stats
        assertEquals(1, stats.total)
        assertEquals(1, stats.known)
        assertEquals(0, stats.unknown)
    }

    @Test
    fun `an unresolved face is counted as seen but neither known nor unknown`() = runTest {
        coEvery { engine.recognise(any(), any(), any()) } returns
            listOf(detection(trackingId = 3, status = MatchStatus.PROCESSING))
        val viewModel = viewModel()

        viewModel.onFrame(frameBitmap, isFrontCamera = false)

        val stats = (viewModel.uiState.value as LiveUiState.Ready).stats
        assertEquals(1, stats.total)
        assertEquals(0, stats.known)
        assertEquals(0, stats.unknown)
    }

    @Test
    fun `writes one alert per stranger, not one per frame`() = runTest {
        coEvery { engine.recognise(any(), any(), any()) } returns
            listOf(detection(trackingId = 7, status = MatchStatus.UNKNOWN))
        val viewModel = viewModel()

        repeat(10) { viewModel.onFrame(frameBitmap, isFrontCamera = false) }

        coVerify(exactly = 1) { alertRepository.save(any()) }
        coVerify(exactly = 1) { eventRepository.save(any()) }
    }

    @Test
    fun `a recognised person raises no alert`() = runTest {
        coEvery { engine.recognise(any(), any(), any()) } returns
            listOf(detection(trackingId = 7, status = MatchStatus.KNOWN, profileId = "ayesha"))
        val viewModel = viewModel()

        viewModel.onFrame(frameBitmap, isFrontCamera = false)

        coVerify(exactly = 0) { alertRepository.save(any()) }
    }

    @Test
    fun `flipping the camera resets the session counters`() = runTest {
        coEvery { engine.recognise(any(), any(), any()) } returns
            listOf(detection(trackingId = 7, status = MatchStatus.UNKNOWN))
        val viewModel = viewModel()
        viewModel.onFrame(frameBitmap, isFrontCamera = false)

        viewModel.flipCamera()

        // Tracking ids restart on the other lens, so carrying counts across would
        // double-count the same person.
        val state = viewModel.uiState.value as LiveUiState.Ready
        assertEquals(SessionStats(), state.stats)
        assertTrue(state.isFrontCamera)
        assertTrue(state.detections.isEmpty())
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

    private fun viewModel() = LiveCameraViewModel(
        prepareRecognition = PrepareRecognitionUseCase(engine, dispatchers),
        recogniseFaces = RecogniseFacesUseCase(engine, dispatchers),
        enrolFaceFromFrame = EnrolFaceFromFrameUseCase(
            engine = engine,
            photoStore = photoStore,
            repository = profileRepository,
            dispatcherProvider = dispatchers,
        ),
        recordUnknownSighting = RecordUnknownSightingUseCase(
            alertRepository = alertRepository,
            detectionEventRepository = eventRepository,
            dispatcherProvider = dispatchers,
        ),
        getEnrolledProfiles = GetEnrolledProfilesUseCase(profileRepository, dispatchers),
        getEnrolledProfileCount = GetEnrolledProfileCountUseCase(profileRepository, dispatchers),
        observeSettings = ObserveSettingsUseCase(settingsRepository, dispatchers),
    )

    private fun detection(
        trackingId: Int,
        status: MatchStatus,
        profileId: String? = null,
    ) = DetectionResult(
        trackingId = trackingId,
        boundingBox = BoundingBox(0.3f, 0.3f, 0.6f, 0.7f),
        matchStatus = status,
        profileId = profileId,
        profileName = profileId,
        confidence = 0.82f,
    )

    private companion object {
        /** Matches the ViewModel's analysis interval, plus a margin. */
        const val THROTTLE_MILLIS = 400L
    }
}
