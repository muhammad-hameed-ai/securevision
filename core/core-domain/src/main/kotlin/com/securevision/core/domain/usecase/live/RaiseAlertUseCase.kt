package com.securevision.core.domain.usecase.live

import com.securevision.core.common.dispatcher.DispatcherProvider
import com.securevision.core.domain.alerting.AlarmPlayer
import com.securevision.core.domain.alerting.AlertGate
import com.securevision.core.domain.alerting.AlertNotifier
import com.securevision.core.domain.alerting.AlertRequest
import com.securevision.core.domain.alerting.NotificationOutcome
import com.securevision.core.domain.repository.AlertRepository
import com.securevision.core.domain.repository.DetectionEventRepository
import com.securevision.core.domain.repository.SettingsRepository
import com.securevision.core.domain.usecase.UseCase
import com.securevision.core.model.AlertRecord
import com.securevision.core.model.DetectionEvent
import com.securevision.core.model.Severity
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * The one way an alert enters the system.
 *
 * Claim → persist → alarm → notify, in that order, and the order is the design:
 *
 * - **Claim first**, so nothing downstream runs for an alert that was
 *   de-duplicated. One [AlertGate] means a suppressed alert is suppressed
 *   everywhere, never recorded-but-silent or sounded-but-unrecorded.
 * - **Persist second and unconditionally.** The audit trail is the part that has
 *   to survive. A tone that will not play and a notification permission the user
 *   refused are both degraded alerts, not lost ones.
 * - **Alarm and notify last**, each behind its own user setting.
 *
 * Replaces the three near-identical `Record*SightingUseCase` classes of Phase 5a,
 * which each held their own copy of the persistence pair. The severity and
 * de-duplication key now come from the [AlertRequest] factories, so the decision
 * that a weapon is critical is written once.
 */
class RaiseAlertUseCase @Inject constructor(
    private val gate: AlertGate,
    private val alertRepository: AlertRepository,
    private val detectionEventRepository: DetectionEventRepository,
    private val settingsRepository: SettingsRepository,
    private val alarmPlayer: AlarmPlayer,
    private val notifier: AlertNotifier,
    dispatcherProvider: DispatcherProvider,
) : UseCase<AlertRequest, AlertOutcome>(dispatcherProvider.io) {

    override suspend fun execute(parameters: AlertRequest): AlertOutcome {
        if (!gate.claim(parameters.dedupKey, parameters.timestamp)) {
            return AlertOutcome.Suppressed
        }

        val record = parameters.toRecord()
        alertRepository.save(record)
        detectionEventRepository.save(parameters.toEvent(record.id))

        val settings = settingsRepository.settingsFlow.first()

        alarmPlayer.play(
            severity = parameters.severity,
            soundEnabled = settings.alertSoundEnabled,
            vibrationEnabled = settings.vibrationEnabled,
        )

        val notification = if (settings.pushNotificationsEnabled) {
            notifier.post(record, parameters.label)
        } else {
            NotificationOutcome.POSTED
        }

        return AlertOutcome.Raised(
            record = record,
            notification = notification,
            // Read after playing rather than inferred from the severity: whether a
            // tone is actually looping depends on the user's sound setting and on
            // the device accepting playback, neither of which the caller can see.
            alarmSounding = alarmPlayer.isSounding,
        )
    }

    private fun AlertRequest.toRecord() = AlertRecord(
        id = UUID.randomUUID().toString(),
        type = type,
        severity = severity,
        confidence = confidence,
        cameraFacing = cameraFacing,
        snapshotUri = snapshotUri,
        // Passed straight through, nulls included. null means "not assessed";
        // coercing it to false would be a claim about a person nothing examined.
        hasBeard = attributes.hasBeard,
        hasMask = attributes.hasMask,
        timestamp = timestamp,
        isRead = false,
    )

    private fun AlertRequest.toEvent(id: String) = DetectionEvent(
        id = id,
        type = type,
        label = label,
        confidence = confidence,
        cameraFacing = cameraFacing,
        timestamp = timestamp,
    )
}

/** What became of a request handed to [RaiseAlertUseCase]. */
sealed interface AlertOutcome {

    /**
     * The gate rejected it as a duplicate.
     *
     * Not a failure: it is the guard doing its job, and the caller should not
     * count it towards session statistics.
     */
    data object Suppressed : AlertOutcome

    /**
     * The alert was recorded.
     *
     * @property record What was written.
     * @property notification Whether the shade actually received it, so the
     *   screen can explain a silent phone instead of leaving the user guessing.
     * @property alarmSounding Whether a repeating tone is now playing, which is
     *   what tells the screen to offer a Silence control.
     */
    data class Raised(
        val record: AlertRecord,
        val notification: NotificationOutcome,
        val alarmSounding: Boolean,
    ) : AlertOutcome
}

/** Whether this outcome represents an alert that was actually recorded. */
val AlertOutcome.wasRaised: Boolean get() = this is AlertOutcome.Raised

/** Convenience for the common "did the severity warrant the loud alarm" question. */
fun AlertOutcome.raisedAtLeast(severity: Severity): Boolean =
    this is AlertOutcome.Raised && record.severity.isAtLeast(severity)
