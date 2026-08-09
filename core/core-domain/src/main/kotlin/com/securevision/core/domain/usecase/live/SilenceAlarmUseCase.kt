package com.securevision.core.domain.usecase.live

import com.securevision.core.domain.alerting.AlarmPlayer
import javax.inject.Inject

/**
 * Stops a repeating critical alarm.
 *
 * Deliberately **not** a [com.securevision.core.domain.usecase.UseCase]. The base
 * class hops to another dispatcher and wraps the call in a `Result`, both of which
 * are wrong here: silencing an alarm is the one action in the app where the user
 * is actively waiting for the noise to stop, and it must not queue behind whatever
 * else is on the IO dispatcher. Stopping playback is already non-blocking.
 */
class SilenceAlarmUseCase @Inject constructor(
    private val alarmPlayer: AlarmPlayer,
) {

    /** Silences the alarm. Safe to call when nothing is sounding. */
    operator fun invoke() {
        alarmPlayer.silence()
    }
}
