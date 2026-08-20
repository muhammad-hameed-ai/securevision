package com.securevision.feature.alerts

import com.securevision.core.common.dispatcher.DispatcherProvider
import com.securevision.core.domain.repository.AlertRepository
import com.securevision.core.domain.usecase.alert.DismissAlertUseCase
import com.securevision.core.domain.usecase.alert.GetAlertsUseCase
import com.securevision.core.domain.usecase.alert.MarkAllAlertsReadUseCase
import com.securevision.core.domain.usecase.alert.RestoreAlertUseCase
import com.securevision.core.domain.usecase.dashboard.GetUnreadAlertCountUseCase
import com.securevision.core.model.AlertRecord
import com.securevision.core.model.AlertType
import com.securevision.core.model.Severity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Filter behaviour, the unread badge, and dismiss/undo. */
class AlertsViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val dispatchers = object : DispatcherProvider {
        override val io: CoroutineDispatcher = dispatcher
        override val default: CoroutineDispatcher = dispatcher
        override val main: CoroutineDispatcher = dispatcher
    }

    private val repository = mockk<AlertRepository>(relaxed = true)
    private val alerts = MutableStateFlow<List<AlertRecord>>(emptyList())

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { repository.getAll() } returns alerts
        every { repository.getUnreadCount() } returns flowOf(0)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `no alerts reports all clear`() = runTest {
        assertEquals(AlertsUiState.Empty, viewModel().uiState.value)
    }

    @Test
    fun `alerts are listed once loaded`() = runTest {
        alerts.value = listOf(alert("1"), alert("2"))

        val state = viewModel().uiState.value as AlertsUiState.Content

        assertEquals(2, state.alerts.size)
    }

    @Test
    fun `the unread filter keeps only unacknowledged alerts`() = runTest {
        alerts.value = listOf(alert("1", isRead = false), alert("2", isRead = true))
        val viewModel = viewModel()

        viewModel.onFilterChange(AlertFilter.UNREAD)

        val state = viewModel.uiState.value as AlertsUiState.Content
        assertEquals(listOf("1"), state.alerts.map(AlertRecord::id))
    }

    @Test
    fun `the weapon filter keeps only weapons`() = runTest {
        alerts.value = listOf(
            alert("1", type = AlertType.WEAPON, severity = Severity.CRITICAL),
            alert("2", type = AlertType.MOTION, severity = Severity.LOW),
        )
        val viewModel = viewModel()

        viewModel.onFilterChange(AlertFilter.WEAPON)

        val state = viewModel.uiState.value as AlertsUiState.Content
        assertEquals(listOf("1"), state.alerts.map(AlertRecord::id))
    }

    @Test
    fun `the critical filter is severity-based, not type-based`() = runTest {
        // A future critical alert that is not a weapon must still appear here.
        alerts.value = listOf(
            alert("1", type = AlertType.UNKNOWN_PERSON, severity = Severity.CRITICAL),
            alert("2", type = AlertType.WEAPON, severity = Severity.HIGH),
        )
        val viewModel = viewModel()

        viewModel.onFilterChange(AlertFilter.CRITICAL)

        val state = viewModel.uiState.value as AlertsUiState.Content
        assertEquals(listOf("1"), state.alerts.map(AlertRecord::id))
    }

    @Test
    fun `a filter that matches nothing is not the all-clear state`() = runTest {
        alerts.value = listOf(alert("1", type = AlertType.MOTION))
        val viewModel = viewModel()

        viewModel.onFilterChange(AlertFilter.WEAPON)

        // Saying "all clear" while a weapon alert sits one chip away would be a lie.
        val state = viewModel.uiState.value as AlertsUiState.Content
        assertTrue(state.isFilteredEmpty)
    }

    @Test
    fun `the unread count ignores the active filter`() = runTest {
        every { repository.getUnreadCount() } returns flowOf(7)
        alerts.value = listOf(alert("1", type = AlertType.MOTION))
        val viewModel = viewModel()

        viewModel.onFilterChange(AlertFilter.WEAPON)

        val state = viewModel.uiState.value as AlertsUiState.Content
        assertEquals(7, state.unreadCount)
    }

    @Test
    fun `mark all read marks rather than deletes`() = runTest {
        alerts.value = listOf(alert("1"))

        viewModel().onMarkAllRead()

        coVerify(exactly = 1) { repository.markAllRead() }
        coVerify(exactly = 0) { repository.delete(any()) }
    }

    @Test
    fun `dismissing removes the alert and offers undo`() = runTest {
        val target = alert("1")
        alerts.value = listOf(target)
        coEvery { repository.getById("1") } returns target
        val viewModel = viewModel()

        viewModel.onDismiss(target)

        coVerify(exactly = 1) { repository.delete("1") }
        assertEquals("1", viewModel.lastDismissed.value?.id)
    }

    @Test
    fun `undo restores the same record`() = runTest {
        val target = alert("1")
        alerts.value = listOf(target)
        coEvery { repository.getById("1") } returns target
        val viewModel = viewModel()
        viewModel.onDismiss(target)

        viewModel.onUndoDismiss()

        // Re-inserted verbatim, so it returns with its original id, timestamp and
        // read state rather than as a new alert.
        coVerify(exactly = 1) { repository.save(target) }
        assertNull(viewModel.lastDismissed.value)
    }

    @Test
    fun `a lapsed undo restores nothing`() = runTest {
        val target = alert("1")
        alerts.value = listOf(target)
        coEvery { repository.getById("1") } returns target
        val viewModel = viewModel()
        viewModel.onDismiss(target)

        viewModel.onUndoExpired()

        assertNull(viewModel.lastDismissed.value)
        coVerify(exactly = 0) { repository.save(any()) }
    }

    /**
     * Builds the ViewModel and subscribes to its state.
     *
     * `stateIn(WhileSubscribed)` produces nothing until something collects, so
     * without this every assertion would read the initial `Loading` value. The
     * collector lives on [TestScope.backgroundScope] so it is cancelled with the
     * test rather than hanging it open.
     */
    private fun TestScope.viewModel() = AlertsViewModel(
        getAlerts = GetAlertsUseCase(repository, dispatchers),
        getUnreadCount = GetUnreadAlertCountUseCase(repository, dispatchers),
        markAllRead = MarkAllAlertsReadUseCase(repository, dispatchers),
        dismissAlert = DismissAlertUseCase(repository, dispatchers),
        restoreAlert = RestoreAlertUseCase(repository, dispatchers),
    ).also { created ->
        backgroundScope.launch(dispatcher) { created.uiState.collect {} }
    }

    private fun alert(
        id: String,
        type: AlertType = AlertType.UNKNOWN_PERSON,
        severity: Severity = Severity.HIGH,
        isRead: Boolean = false,
    ) = AlertRecord(
        id = id,
        type = type,
        severity = severity,
        confidence = 0.8f,
        cameraFacing = "back",
        snapshotUri = "file:///snapshots/$id.jpg",
        hasBeard = null,
        hasMask = null,
        timestamp = 1_700_000_000_000L,
        isRead = isRead,
    )
}
