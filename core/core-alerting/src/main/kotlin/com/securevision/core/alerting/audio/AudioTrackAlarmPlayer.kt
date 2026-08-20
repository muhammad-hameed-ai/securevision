package com.securevision.core.alerting.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import com.securevision.core.domain.alerting.AlarmPlayer
import com.securevision.core.model.Severity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Plays the synthesized alarm through an `AudioTrack`.
 *
 * Uses a **static** track rather than a streaming one: the whole tone cycle is
 * short enough to sit in the buffer, and the hardware can then loop it with no
 * writer thread of its own. A streaming track would need a coroutine feeding it
 * for as long as a critical alarm sounds, which is a thread that can be starved
 * by the very detection work that raised the alarm.
 *
 * The tone itself is [AlarmToneSynth]'s arithmetic; this class only moves bytes
 * and manages focus.
 */
@Singleton
class AudioTrackAlarmPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val vibrator: AlertVibrator,
) : AlarmPlayer {

    private val audioManager: AudioManager? by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        context.getSystemService(AudioManager::class.java)
    }

    private val attributes = AudioAttributes.Builder()
        // USAGE_ALARM, not USAGE_NOTIFICATION: an alarm is routed to the alarm
        // stream, which stays audible under Do Not Disturb and is not silenced by
        // the notification volume the user turned down last week.
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private var track: AudioTrack? = null
    private var focusRequest: AudioFocusRequest? = null

    @Volatile
    private var sounding = false

    /**
     * Which severity owns the tone currently playing, or `null` when silent.
     *
     * The whole point of tracking it: a request from a lower severity is refused
     * rather than allowed to release the track. Only the user's Silence button,
     * or an equal-or-higher severity, can take over.
     */
    @Volatile
    private var soundingSeverity: Severity? = null

    override val isSounding: Boolean get() = sounding

    override suspend fun play(
        severity: Severity,
        soundEnabled: Boolean,
        vibrationEnabled: Boolean,
    ) {
        if (vibrationEnabled) vibrator.vibrate(severity)

        if (!soundEnabled) {
            Log.i(TAG, "alarm suppressed: sound is switched off in settings")
            return
        }

        // A lower-severity alert must never take the speaker from a higher one.
        // Without this an unknown-person chime tore down a sounding weapon alarm
        // — `start()` releases the current track before every tone — so the one
        // alarm that matters most was silenced by the one that matters least.
        val owner = soundingSeverity
        if (owner != null && owner.isAtLeast(severity) && owner != severity) {
            Log.i(TAG, "$severity tone refused: a $owner alarm owns the speaker")
            return
        }

        val cycle = AlarmToneSynth.cycleFor(severity)
        if (cycle.isEmpty()) {
            Log.i(TAG, "no tone defined for $severity — silent by design")
            return
        }

        // The single most common reason an alarm is inaudible while the phone
        // seems fine: the tone is routed to the ALARM stream, which has its own
        // volume separate from media and ringer. Reported rather than worked
        // around — overriding the user's own volume setting would be worse.
        val alarmVolume = audioManager?.getStreamVolume(AudioManager.STREAM_ALARM) ?: -1
        if (alarmVolume == 0) {
            Log.w(TAG, "alarm stream volume is 0 — the tone will play but be inaudible")
        }

        Log.i(TAG, "playing $severity tone, ${cycle.size} samples, alarm volume $alarmVolume")

        runCatching { start(cycle, severity, repeating = AlarmToneSynth.repeats(severity)) }
            .onFailure { throwable ->
                // A degraded alert, not a failed one: the record is already
                // written, so the alarm going quiet must not propagate.
                Log.w(TAG, "alarm playback failed", throwable)
                silence()
            }
    }

    override fun silence() {
        synchronized(this) {
            sounding = false
            // Silence is the user's override, so it releases ownership whatever
            // severity holds it.
            soundingSeverity = null

            track?.let { active ->
                runCatching {
                    active.pause()
                    active.flush()
                    active.stop()
                }.onFailure { throwable -> Log.w(TAG, "stopping alarm failed", throwable) }

                runCatching { active.release() }
                    .onFailure { throwable -> Log.w(TAG, "releasing alarm failed", throwable) }
            }
            track = null

            abandonFocus()
        }
        vibrator.cancel()
    }

    private fun start(cycle: ShortArray, severity: Severity, repeating: Boolean) {
        synchronized(this) {
            // A second critical detection while the alarm is already sounding must
            // not stack a second track on top of the first — but it must also not
            // restart it, which is what lets the weapon alarm keep running
            // continuously while the weapon stays in frame.
            if (sounding && repeating) return

            releaseCurrentLocked()
            requestFocus()

            val sizeBytes = cycle.size * BYTES_PER_SAMPLE

            val newTrack = AudioTrack.Builder()
                .setAudioAttributes(attributes)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(AlarmToneSynth.SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(sizeBytes)
                .build()

            newTrack.write(cycle, 0, cycle.size)

            if (repeating) {
                // -1 loops until stopped, which is what "until silenced" means.
                newTrack.setLoopPoints(0, cycle.size, LOOP_FOREVER)
            }

            newTrack.play()

            // The state after play() is the ground truth. A track that failed to
            // start reports STOPPED here, which is the difference between "the
            // code ran" and "the device made a sound".
            Log.i(TAG, "AudioTrack state=${newTrack.state} playState=${newTrack.playState}")

            track = newTrack
            sounding = repeating
            // Only a repeating tone owns the speaker. A one-shot chime finishes
            // on its own and must not lock out anything that follows it.
            soundingSeverity = severity.takeIf { repeating }
        }
    }

    private fun releaseCurrentLocked() {
        track?.let { previous ->
            runCatching {
                previous.pause()
                previous.flush()
                previous.stop()
                previous.release()
            }.onFailure { throwable -> Log.w(TAG, "replacing alarm track failed", throwable) }
        }
        track = null
    }

    private fun requestFocus() {
        val manager = audioManager ?: return
        if (focusRequest != null) return

        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(attributes)
            .build()

        runCatching { manager.requestAudioFocus(request) }
            .onSuccess { result ->
                focusRequest = request
                Log.i(TAG, "audio focus request returned $result")
            }
            .onFailure { throwable ->
                // Play anyway. Failing to duck the user's music is a far smaller
                // problem than a weapon alarm that never sounds.
                Log.w(TAG, "audio focus request failed", throwable)
            }
    }

    private fun abandonFocus() {
        val manager = audioManager
        val request = focusRequest ?: return
        focusRequest = null

        // Not abandoning would leave the user's music ducked indefinitely, long
        // after the alarm they silenced.
        runCatching { manager?.abandonAudioFocusRequest(request) }
            .onFailure { throwable -> Log.w(TAG, "abandoning audio focus failed", throwable) }
    }

    private companion object {
        const val TAG = "AlarmPlayer"
        const val BYTES_PER_SAMPLE = 2
        const val LOOP_FOREVER = -1
    }
}
