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

    override val isSounding: Boolean get() = sounding

    override suspend fun play(
        severity: Severity,
        soundEnabled: Boolean,
        vibrationEnabled: Boolean,
    ) {
        if (vibrationEnabled) vibrator.vibrate(severity)
        if (!soundEnabled) return

        val cycle = AlarmToneSynth.cycleFor(severity)
        if (cycle.isEmpty()) return

        runCatching { start(cycle, repeating = AlarmToneSynth.repeats(severity)) }
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

    private fun start(cycle: ShortArray, repeating: Boolean) {
        synchronized(this) {
            // A second critical detection while the alarm is already sounding must
            // not stack a second track on top of the first.
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

            track = newTrack
            sounding = repeating
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
            .onSuccess { focusRequest = request }
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
