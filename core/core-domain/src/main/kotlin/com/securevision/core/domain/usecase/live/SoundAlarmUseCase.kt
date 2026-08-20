package com.securevision.core.domain.usecase.live

import com.securevision.core.domain.alerting.AlarmPlayer
import com.securevision.core.domain.repository.SettingsRepository
import com.securevision.core.model.Severity
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * Sounds the alarm for a danger that is still present, without recording it again.
 *
 * Deliberately separate from [RaiseAlertUseCase]. That path is gated by
 * [com.securevision.core.domain.alerting.AlertGate], which claims a key once and
 * never again — right for the alert *record*, since nobody wants forty rows for
 * one weapon, but wrong for the *alarm*: it meant the tone sounded once and fell
 * silent while the weapon was still in frame.
 *
 * An alarm answers "is there danger right now", a record answers "what happened".
 * They have different lifetimes and this is the one that follows the danger.
 *
 * The player ignores a re-arm while the same tone is already looping, so calling
 * this every cycle is cheap and cannot stack tracks.
 */
class SoundAlarmUseCase @Inject constructor(
    private val alarmPlayer: AlarmPlayer,
    private val settingsRepository: SettingsRepository,
) {

    /**
     * Sounds, or keeps sounding, the alarm for [severity].
     *
     * @param severity Urgency of the danger still present.
     */
    suspend operator fun invoke(severity: Severity) {
        val settings = settingsRepository.settingsFlow.first()

        alarmPlayer.play(
            severity = severity,
            soundEnabled = settings.alertSoundEnabled,
            // Re-arming does not re-vibrate: a continuous buzz while a weapon
            // stays in frame is unbearable, and the tone already carries it.
            vibrationEnabled = false,
        )
    }
}
