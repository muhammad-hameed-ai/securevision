package com.securevision.feature.profiles

import com.securevision.core.common.dispatcher.DispatcherProvider
import com.securevision.core.domain.engine.AttributeAnalysisEngine
import com.securevision.core.domain.engine.EngineStatus
import com.securevision.core.domain.engine.FaceRecognitionEngine
import com.securevision.core.domain.engine.InferenceDelegate
import com.securevision.core.domain.engine.MotionDetectionEngine
import com.securevision.core.domain.engine.ProfilePhotoStore
import com.securevision.core.domain.engine.WeaponDetectionEngine
import com.securevision.core.domain.repository.EnrolledProfileRepository
import com.securevision.core.domain.usecase.live.PrepareDetectorsUseCase
import com.securevision.core.domain.usecase.profile.CaptureEnrolmentUseCase
import com.securevision.core.domain.usecase.profile.SaveEnrolmentUseCase
import com.securevision.core.domain.usecase.profile.UpdateProfileUseCase
import com.securevision.core.model.AttributeAvailability
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Enrolment must load the face model itself.
 *
 * The field report was "the face model is not installed" on a device where
 * `facenet_512.tflite` was present and working. The cause: `prepare()` had one
 * caller — the live camera — so opening People → Add Person without visiting
 * Live meant nothing ever asked for the model, and `embedForEnrolment` refused
 * with MODEL_UNAVAILABLE. This test is the guard against that regressing: the
 * enrolment screen is responsible for its own dependency.
 */
class ColdEnrolmentPreparesModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val dispatchers = object : DispatcherProvider {
        override val io: CoroutineDispatcher = dispatcher
        override val default: CoroutineDispatcher = dispatcher
        override val main: CoroutineDispatcher = dispatcher
    }

    private val faceEngine = mockk<FaceRecognitionEngine>(relaxed = true)
    private val weaponEngine = mockk<WeaponDetectionEngine>(relaxed = true)
    private val attributeEngine = mockk<AttributeAnalysisEngine>(relaxed = true)
    private val motionEngine = mockk<MotionDetectionEngine>(relaxed = true)
    private val photoStore = mockk<ProfilePhotoStore>(relaxed = true)
    private val repository = mockk<EnrolledProfileRepository>(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)

        every { repository.getAll() } returns flowOf(emptyList())
        coEvery { faceEngine.prepare(any()) } returns READY
        coEvery { weaponEngine.prepare() } returns READY
        coEvery { attributeEngine.prepare() } returns AttributeAvailability()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `opening the screen prepares the face model`() {
        viewModel()

        // The whole bug in one assertion: nothing used to call this from here.
        coVerify(exactly = 1) { faceEngine.prepare(any()) }
    }

    @Test
    fun `a prepared model leaves the screen usable`() {
        val state = viewModel().uiState.value

        assertFalse(state.isPreparing)
        assertFalse(state.modelUnavailable)
        assertTrue(state.canCapture)
    }

    @Test
    fun `a model that genuinely fails to load is reported as unavailable`() {
        coEvery { faceEngine.prepare(any()) } returns EngineStatus.RecognitionUnavailable(
            EngineStatus.RecognitionUnavailable.Reason.MODEL_NOT_INSTALLED,
        )

        val state = viewModel().uiState.value

        // Only now is the message honest: the model was asked for and refused.
        assertTrue(state.modelUnavailable)
        assertFalse(state.canCapture)
    }

    @Test
    fun `the shutter is withheld while the model is still loading`() = runTest {
        // Preparation is not instant on a real device, and offering a shutter that
        // cannot work is worse than briefly offering none.
        val preparing = AddProfileUiState(isPreparing = true)

        assertFalse(preparing.canCapture)
    }

    @Test
    fun `the enrolled dimension is passed so a model swap is detected`() {
        viewModel()

        // An empty database passes null, which is correct — there is nothing to
        // be incompatible with yet.
        coVerify { faceEngine.prepare(null) }
    }

    private fun viewModel() = AddProfileViewModel(
        prepareDetectors = PrepareDetectorsUseCase(
            faceEngine = faceEngine,
            weaponEngine = weaponEngine,
            attributeEngine = attributeEngine,
            motionEngine = motionEngine,
            dispatcherProvider = dispatchers,
        ),
        captureEnrolment = CaptureEnrolmentUseCase(faceEngine, dispatchers),
        saveEnrolment = SaveEnrolmentUseCase(photoStore, repository, dispatchers),
        updateProfile = UpdateProfileUseCase(repository, dispatchers),
        repository = repository,
    )

    private companion object {
        val READY = EngineStatus.Ready(
            embeddingDimensions = 512,
            delegate = InferenceDelegate.CPU,
        )
    }
}
