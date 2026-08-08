package com.securevision.feature.dashboard

import app.cash.turbine.test
import com.securevision.core.common.dispatcher.DispatcherProvider
import com.securevision.core.domain.repository.AlertRepository
import com.securevision.core.domain.repository.DetectionEventRepository
import com.securevision.core.domain.repository.EnrolledProfileRepository
import com.securevision.core.domain.usecase.alert.GetRecentAlertsUseCase
import com.securevision.core.domain.usecase.dashboard.GetDetectionEventCountUseCase
import com.securevision.core.domain.usecase.dashboard.GetEnrolledProfileCountUseCase
import com.securevision.core.domain.usecase.dashboard.GetUnreadAlertCountUseCase
import com.securevision.core.model.AlertRecord
import com.securevision.core.model.AlertType
import com.securevision.core.model.Severity
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Covers how the four independent sources are reduced into one screen state.
 *
 * `Dispatchers.Main` is replaced because `stateInWhileSubscribed` shares on
 * `viewModelScope`, which is main-dispatched.
 */
class DashboardViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val alertRepository = mockk<AlertRepository>()
    private val profileRepository = mockk<EnrolledProfileRepository>()
    private val eventRepository = mockk<DetectionEventRepository>()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `starts in Loading before any source emits`() = runTest {
        stubSources(
            unreadCount = neverEmitting(),
            profileCount = neverEmitting(),
            eventCount = neverEmitting(),
            recentAlerts = neverEmitting(),
        )

        assertEquals(DashboardUiState.Loading, viewModel().uiState.value)
    }

    @Test
    fun `emits Content with zeros and no alerts on a fresh install`() = runTest {
        stubSources(
            unreadCount = flowOf(0),
            profileCount = flowOf(0),
            eventCount = flowOf(0),
            recentAlerts = flowOf(emptyList()),
        )

        viewModel().uiState.test {
            val state = awaitItem()

            assertEquals(
                DashboardUiState.Content(
                    unreadAlerts = 0,
                    profileCount = 0,
                    eventCount = 0,
                    recentAlerts = emptyList(),
                ),
                state,
            )
            assertTrue((state as DashboardUiState.Content).hasNoAlerts)
        }
    }

    @Test
    fun `emits Content carrying every figure once all sources have emitted`() = runTest {
        val alert = alert()
        stubSources(
            unreadCount = flowOf(3),
            profileCount = flowOf(12),
            eventCount = flowOf(148),
            recentAlerts = flowOf(listOf(alert)),
        )

        viewModel().uiState.test {
            val state = awaitItem() as DashboardUiState.Content

            assertEquals(3, state.unreadAlerts)
            assertEquals(12, state.profileCount)
            assertEquals(148, state.eventCount)
            assertEquals(listOf(alert), state.recentAlerts)
        }
    }

    @Test
    fun `stays in Loading while any single source has not emitted`() = runTest {
        stubSources(
            unreadCount = flowOf(3),
            profileCount = flowOf(12),
            eventCount = flowOf(148),
            recentAlerts = neverEmitting(),
        )

        viewModel().uiState.test {
            assertEquals(DashboardUiState.Loading, awaitItem())
        }
    }

    @Test
    fun `surfaces Error rather than defaulting a failed source to zero`() = runTest {
        stubSources(
            unreadCount = flowOf(3),
            profileCount = flow { throw IllegalStateException("database unavailable") },
            eventCount = flowOf(148),
            recentAlerts = flowOf(emptyList()),
        )

        viewModel().uiState.test {
            // A silently zeroed profile count would be indistinguishable from
            // "nobody is enrolled", which is a plausible and wrong reading.
            assertEquals(
                DashboardUiState.Error("database unavailable"),
                awaitItem(),
            )
        }
    }

    private fun stubSources(
        unreadCount: Flow<Int>,
        profileCount: Flow<Int>,
        eventCount: Flow<Int>,
        recentAlerts: Flow<List<AlertRecord>>,
    ) {
        every { alertRepository.getUnreadCount() } returns unreadCount
        every { profileRepository.countAll() } returns profileCount
        every { eventRepository.countAll() } returns eventCount
        every { alertRepository.getRecent(any()) } returns recentAlerts
    }

    private fun viewModel(): DashboardViewModel {
        val dispatchers = object : DispatcherProvider {
            override val io: CoroutineDispatcher = testDispatcher
            override val default: CoroutineDispatcher = testDispatcher
            override val main: CoroutineDispatcher = testDispatcher
        }

        return DashboardViewModel(
            getUnreadAlertCount = GetUnreadAlertCountUseCase(alertRepository, dispatchers),
            getEnrolledProfileCount = GetEnrolledProfileCountUseCase(profileRepository, dispatchers),
            getDetectionEventCount = GetDetectionEventCountUseCase(eventRepository, dispatchers),
            getRecentAlerts = GetRecentAlertsUseCase(alertRepository, dispatchers),
        )
    }

    /** A flow that never emits, standing in for a source that has not loaded yet. */
    private fun <T> neverEmitting(): Flow<T> = flow { kotlinx.coroutines.awaitCancellation() }

    private fun alert() = AlertRecord(
        id = "a1",
        type = AlertType.UNKNOWN_PERSON,
        severity = Severity.CRITICAL,
        confidence = 0.91f,
        cameraFacing = "front",
        snapshotUri = null,
        hasBeard = true,
        hasMask = false,
        timestamp = 1_754_000_000_000L,
        isRead = false,
    )
}
