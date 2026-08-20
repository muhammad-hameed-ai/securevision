package com.securevision.feature.recordings

import com.securevision.core.common.dispatcher.DispatcherProvider
import com.securevision.core.domain.repository.RecordingRepository
import com.securevision.core.domain.usecase.recording.DeleteRecordingUseCase
import com.securevision.core.domain.usecase.recording.GetRecordingsUseCase
import com.securevision.core.model.Recording
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/** Gallery state and confirmed deletion. */
class RecordingsViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val dispatchers = object : DispatcherProvider {
        override val io: CoroutineDispatcher = dispatcher
        override val default: CoroutineDispatcher = dispatcher
        override val main: CoroutineDispatcher = dispatcher
    }

    private val repository = mockk<RecordingRepository>(relaxed = true)
    private val recordings = MutableStateFlow<List<Recording>>(emptyList())

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { repository.getAll() } returns recordings
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `no clips reports empty`() = runTest {
        assertEquals(RecordingsUiState.Empty, viewModel().uiState.value)
    }

    @Test
    fun `clips are listed once loaded`() = runTest {
        recordings.value = listOf(recording("1"), recording("2"))

        val state = viewModel().uiState.value as RecordingsUiState.Content

        assertEquals(2, state.recordings.size)
    }

    @Test
    fun `deleting asks first`() = runTest {
        recordings.value = listOf(recording("1"))
        val viewModel = viewModel()

        viewModel.onDeleteRequested(recordings.value.first())

        assertEquals("1", viewModel.pendingDeletion.value?.id)
        coVerify(exactly = 0) { repository.delete(any()) }
    }

    @Test
    fun `confirming removes the clip and its file`() = runTest {
        recordings.value = listOf(recording("1"))
        val viewModel = viewModel()
        viewModel.onDeleteRequested(recordings.value.first())

        viewModel.onDeleteConfirmed()

        // The repository deletes the row and the video together; an orphaned file
        // would be the largest thing this app leaks.
        coVerify(exactly = 1) { repository.delete("1") }
        assertNull(viewModel.pendingDeletion.value)
    }

    @Test
    fun `cancelling keeps the clip`() = runTest {
        recordings.value = listOf(recording("1"))
        val viewModel = viewModel()
        viewModel.onDeleteRequested(recordings.value.first())

        viewModel.onDeleteCancelled()

        assertNull(viewModel.pendingDeletion.value)
        coVerify(exactly = 0) { repository.delete(any()) }
    }

    /**
     * Builds the ViewModel and subscribes to its state.
     *
     * `stateIn(WhileSubscribed)` emits nothing until something collects, so
     * without this every assertion would read the initial `Loading` value.
     */
    private fun TestScope.viewModel() = RecordingsViewModel(
        getRecordings = GetRecordingsUseCase(repository, dispatchers),
        deleteRecording = DeleteRecordingUseCase(repository, dispatchers),
    ).also { created ->
        backgroundScope.launch(dispatcher) { created.uiState.collect {} }
    }

    private fun recording(id: String) = Recording(
        id = id,
        filePath = "/data/recordings/$id.mp4",
        durationMs = 12_000L,
        thumbnailUri = null,
        createdAt = 1_700_000_000_000L,
    )
}
