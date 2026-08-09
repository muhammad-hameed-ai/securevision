package com.securevision.core.domain.usecase.live

import com.securevision.core.common.dispatcher.DispatcherProvider
import com.securevision.core.common.result.getOrNull
import com.securevision.core.domain.alerting.AlarmPlayer
import com.securevision.core.domain.alerting.AlertGate
import com.securevision.core.domain.alerting.AlertNotifier
import com.securevision.core.domain.alerting.AlertRequest
import com.securevision.core.domain.alerting.NotificationOutcome
import com.securevision.core.domain.repository.AlertRepository
import com.securevision.core.domain.repository.DetectionEventRepository
import com.securevision.core.domain.repository.SettingsRepository
import com.securevision.core.model.AlertRecord
import com.securevision.core.model.AppSettings
import com.securevision.core.model.FaceAttributes
import com.securevision.core.model.Severity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * The single alerting path.
 *
 * The load-bearing assertion here is that the record survives everything: a tone
 * that will not play and a notification permission the user refused are degraded
 * alerts, never lost ones.
 */
class RaiseAlertUseCaseTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val dispatchers = object : DispatcherProvider {
        override val io: CoroutineDispatcher = dispatcher
        override val default: CoroutineDispatcher = dispatcher
        override val main: CoroutineDispatcher = dispatcher
    }

    private val gate = AlertGate()
    private val alertRepository = mockk<AlertRepository>(relaxed = true)
    private val eventRepository = mockk<DetectionEventRepository>(relaxed = true)
    private val settingsRepository = mockk<SettingsRepository>(relaxed = true)
    private val alarmPlayer = mockk<AlarmPlayer>(relaxed = true)
    private val notifier = mockk<AlertNotifier>(relaxed = true)

    @Before
    fun setUp() {
        every { settingsRepository.settingsFlow } returns flowOf(AppSettings())
        every { alarmPlayer.isSounding } returns false
        coEvery { notifier.post(any(), any()) } returns NotificationOutcome.POSTED
    }

    @Test
    fun `a claimed alert is persisted, sounded and notified`() = runTest {
        val outcome = useCase()(weaponRequest()).getOrNull()

        assertTrue(outcome is AlertOutcome.Raised)
        coVerify(exactly = 1) { alertRepository.save(any()) }
        coVerify(exactly = 1) { eventRepository.save(any()) }
        coVerify(exactly = 1) { alarmPlayer.play(Severity.CRITICAL, true, true) }
        coVerify(exactly = 1) { notifier.post(any(), "pistol") }
    }

    @Test
    fun `a duplicate is suppressed everywhere, not merely left unrecorded`() = runTest {
        val subject = useCase()
        subject(weaponRequest())

        val outcome = subject(weaponRequest()).getOrNull()

        assertEquals(AlertOutcome.Suppressed, outcome)
        // One of each, not two. A suppressed alert must not sound or notify either
        // — that is the whole point of a single gate.
        coVerify(exactly = 1) { alertRepository.save(any()) }
        coVerify(exactly = 1) { alarmPlayer.play(any(), any(), any()) }
        coVerify(exactly = 1) { notifier.post(any(), any()) }
    }

    @Test
    fun `the record survives a refused notification permission`() = runTest {
        coEvery { notifier.post(any(), any()) } returns NotificationOutcome.PERMISSION_DENIED

        val outcome = useCase()(weaponRequest()).getOrNull()

        coVerify(exactly = 1) { alertRepository.save(any()) }
        assertEquals(
            NotificationOutcome.PERMISSION_DENIED,
            (outcome as AlertOutcome.Raised).notification,
        )
    }

    @Test
    fun `the record survives an alarm that throws`() = runTest {
        // The contract says implementations never throw. If one does anyway, the
        // audit trail is the thing that must not be lost.
        coEvery { alarmPlayer.play(any(), any(), any()) } throws IllegalStateException("no audio")

        useCase()(weaponRequest())

        coVerify(exactly = 1) { alertRepository.save(any()) }
        coVerify(exactly = 1) { eventRepository.save(any()) }
    }

    @Test
    fun `the sound setting is honoured`() = runTest {
        every { settingsRepository.settingsFlow } returns
            flowOf(AppSettings(alertSoundEnabled = false, vibrationEnabled = false))

        useCase()(weaponRequest())

        coVerify(exactly = 1) { alarmPlayer.play(Severity.CRITICAL, false, false) }
    }

    @Test
    fun `notifications are skipped when the user has switched them off`() = runTest {
        every { settingsRepository.settingsFlow } returns
            flowOf(AppSettings(pushNotificationsEnabled = false))

        useCase()(weaponRequest())

        coVerify(exactly = 0) { notifier.post(any(), any()) }
        // Still recorded: the toggle silences the phone, not the audit trail.
        coVerify(exactly = 1) { alertRepository.save(any()) }
    }

    @Test
    fun `unassessed attributes reach the record as null, never false`() = runTest {
        val saved = slot<AlertRecord>()

        useCase()(
            AlertRequest.unknownPerson(
                trackingId = 4,
                confidence = 0.4f,
                cameraFacing = "front",
                attributes = FaceAttributes.NOT_ASSESSED,
            ),
        )

        coVerify { alertRepository.save(capture(saved)) }
        assertNull(saved.captured.hasBeard)
        assertNull(saved.captured.hasMask)
    }

    @Test
    fun `severity comes from the request factory, not the caller`() = runTest {
        val saved = slot<AlertRecord>()
        val subject = useCase()

        subject(AlertRequest.motion(intensity = 0.3f, cameraFacing = "back"))

        coVerify { alertRepository.save(capture(saved)) }
        assertEquals(Severity.LOW, saved.captured.severity)
    }

    @Test
    fun `the alert and its audit event share one id`() = runTest {
        val alert = slot<AlertRecord>()
        val event = slot<com.securevision.core.model.DetectionEvent>()

        useCase()(weaponRequest())

        coVerify { alertRepository.save(capture(alert)) }
        coVerify { eventRepository.save(capture(event)) }
        // They are two views of one occurrence; a mismatch would break any future
        // screen that opens the event behind an alert.
        assertEquals(alert.captured.id, event.captured.id)
    }

    private fun useCase() = RaiseAlertUseCase(
        gate = gate,
        alertRepository = alertRepository,
        detectionEventRepository = eventRepository,
        settingsRepository = settingsRepository,
        alarmPlayer = alarmPlayer,
        notifier = notifier,
        dispatcherProvider = dispatchers,
    )

    private fun weaponRequest() = AlertRequest.weapon(
        weaponType = "pistol",
        confidence = 0.93f,
        cameraFacing = "back",
        timestamp = 1_700_000_000_000L,
    )
}
