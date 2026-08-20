package com.securevision.feature.profiles

import com.securevision.core.common.dispatcher.DispatcherProvider
import com.securevision.core.domain.repository.EnrolledProfileRepository
import com.securevision.core.domain.usecase.profile.DeleteProfileUseCase
import com.securevision.core.domain.usecase.profile.GetEnrolledProfilesUseCase
import com.securevision.core.model.AccessLevel
import com.securevision.core.model.EnrolledProfile
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** State transitions and the two filters, which compose rather than replace. */
class ProfilesViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val dispatchers = object : DispatcherProvider {
        override val io: CoroutineDispatcher = dispatcher
        override val default: CoroutineDispatcher = dispatcher
        override val main: CoroutineDispatcher = dispatcher
    }

    private val repository = mockk<EnrolledProfileRepository>(relaxed = true)
    private val profiles = MutableStateFlow<List<EnrolledProfile>>(emptyList())

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { repository.getAll() } returns profiles
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `an empty database reports empty, not an empty list`() = runTest {
        // Distinct states: nobody enrolled invites enrolment, whereas an empty
        // filtered list should not.
        assertTrue(viewModel().uiState.value is ProfilesUiState.Empty)
    }

    @Test
    fun `profiles are shown once loaded`() = runTest {
        profiles.value = listOf(profile("1", "Ayesha"), profile("2", "Bilal"))

        val state = viewModel().uiState.value as ProfilesUiState.Content

        assertEquals(2, state.profiles.size)
        assertEquals(2, state.totalCount)
    }

    @Test
    fun `search filters by name, case-insensitively`() = runTest {
        profiles.value = listOf(profile("1", "Ayesha"), profile("2", "Bilal"))
        val viewModel = viewModel()

        viewModel.onQueryChange("ayes")

        val state = viewModel.uiState.value as ProfilesUiState.Content
        assertEquals(listOf("Ayesha"), state.profiles.map(EnrolledProfile::name))
    }

    @Test
    fun `a search matching nobody is filtered-empty, not empty`() = runTest {
        profiles.value = listOf(profile("1", "Ayesha"))
        val viewModel = viewModel()

        viewModel.onQueryChange("zzz")

        val state = viewModel.uiState.value as ProfilesUiState.Content
        assertTrue(state.isFilteredEmpty)
        // Telling someone who is searching to go and enrol people would answer a
        // question they did not ask.
        assertEquals(1, state.totalCount)
    }

    @Test
    fun `the watchlist filter narrows to watchlisted people`() = runTest {
        profiles.value = listOf(
            profile("1", "Ayesha", watchlisted = true),
            profile("2", "Bilal"),
        )
        val viewModel = viewModel()

        viewModel.onWatchlistFilterToggle()

        val state = viewModel.uiState.value as ProfilesUiState.Content
        assertEquals(listOf("Ayesha"), state.profiles.map(EnrolledProfile::name))
    }

    @Test
    fun `search and the watchlist filter compose`() = runTest {
        profiles.value = listOf(
            profile("1", "Ayesha", watchlisted = true),
            profile("2", "Ayaan", watchlisted = false),
            profile("3", "Bilal", watchlisted = true),
        )
        val viewModel = viewModel()

        viewModel.onWatchlistFilterToggle()
        viewModel.onQueryChange("ay")

        // Both filters apply. If one replaced the other this would return two.
        val state = viewModel.uiState.value as ProfilesUiState.Content
        assertEquals(listOf("Ayesha"), state.profiles.map(EnrolledProfile::name))
    }

    @Test
    fun `deleting asks before destroying an embedding`() = runTest {
        profiles.value = listOf(profile("1", "Ayesha"))
        val viewModel = viewModel()

        viewModel.onDeleteRequested(profiles.value.first())

        assertEquals("Ayesha", viewModel.pendingDeletion.value?.name)
        coVerify(exactly = 0) { repository.delete(any()) }
    }

    @Test
    fun `cancelling a deletion keeps the profile`() = runTest {
        profiles.value = listOf(profile("1", "Ayesha"))
        val viewModel = viewModel()
        viewModel.onDeleteRequested(profiles.value.first())

        viewModel.onDeleteCancelled()

        assertEquals(null, viewModel.pendingDeletion.value)
        coVerify(exactly = 0) { repository.delete(any()) }
    }

    @Test
    fun `confirming a deletion removes the profile`() = runTest {
        profiles.value = listOf(profile("1", "Ayesha"))
        val viewModel = viewModel()
        viewModel.onDeleteRequested(profiles.value.first())

        viewModel.onDeleteConfirmed()

        coVerify(exactly = 1) { repository.delete("1") }
        assertEquals(null, viewModel.pendingDeletion.value)
    }

    @Test
    fun `confirming with nothing pending does nothing`() = runTest {
        val viewModel = viewModel()

        viewModel.onDeleteConfirmed()

        coVerify(exactly = 0) { repository.delete(any()) }
    }

    @Test
    fun `an unfiltered list reports no active filter`() = runTest {
        profiles.value = listOf(profile("1", "Ayesha"))

        val state = viewModel().uiState.value as ProfilesUiState.Content

        assertFalse(state.isFiltered)
    }

    /**
     * Builds the ViewModel and subscribes to its state.
     *
     * `stateIn(WhileSubscribed)` emits nothing until something collects, so
     * without this every assertion would read the initial `Loading` value.
     */
    private fun TestScope.viewModel() = ProfilesViewModel(
        getEnrolledProfiles = GetEnrolledProfilesUseCase(repository, dispatchers),
        deleteProfile = DeleteProfileUseCase(repository, dispatchers),
    ).also { created ->
        backgroundScope.launch(dispatcher) { created.uiState.collect {} }
    }

    private fun profile(
        id: String,
        name: String,
        watchlisted: Boolean = false,
    ) = EnrolledProfile(
        id = id,
        name = name,
        age = 30,
        photoUri = "file:///profiles/$id.jpg",
        embedding = FloatArray(512) { 0.01f },
        accessLevel = AccessLevel.STANDARD,
        isWatchlisted = watchlisted,
        createdAt = 1_700_000_000_000L,
    )
}
