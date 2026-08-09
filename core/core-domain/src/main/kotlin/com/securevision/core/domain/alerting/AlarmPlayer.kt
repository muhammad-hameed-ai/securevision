package com.securevision.core.domain.alerting

import com.securevision.core.model.Severity

/**
 * The audible and haptic side of an alert.
 *
 * Declared here so the domain can raise an alarm without knowing that Android has
 * an `AudioTrack` or a `Vibrator`. `feature-live` reaches it only through a use
 * case, exactly as it reaches the detectors.
 *
 * Implementations must **never throw**. A tone that fails to play is a degraded
 * alert, not a failed one, and the persisted record must survive it — mirroring
 * how [com.securevision.core.domain.engine.SnapshotStore] returns `null` rather
 * than propagating a write failure.
 */
interface AlarmPlayer {

    /** Whether a repeating alarm is currently sounding, so the UI can offer Silence. */
    val isSounding: Boolean

    /**
     * Sounds the alarm appropriate to a severity.
     *
     * [Severity.CRITICAL] repeats until [silence] is called; everything else is a
     * single short cue. Below [Severity.HIGH] nothing is played at all: motion
     * fires on any movement in a static scene, and a phone that chirps at a
     * curtain teaches its owner to ignore it.
     *
     * @param severity Urgency of the alert being announced.
     * @param soundEnabled User's alarm-sound setting.
     * @param vibrationEnabled User's vibration setting.
     */
    suspend fun play(severity: Severity, soundEnabled: Boolean, vibrationEnabled: Boolean)

    /** Stops a repeating alarm and releases audio focus. Safe to call when silent. */
    fun silence()
}
