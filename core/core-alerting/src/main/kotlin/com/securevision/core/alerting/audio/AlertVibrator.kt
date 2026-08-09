package com.securevision.core.alerting.audio

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.securevision.core.model.Severity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Haptic half of an alert.
 *
 * Split from the tone because the two fail independently: a device with no
 * vibrator still needs the alarm to sound, and a muted phone still needs the
 * pattern. Neither is allowed to take the other down.
 */
@Singleton
class AlertVibrator @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val vibrator: Vibrator? by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        runCatching { resolveVibrator() }
            .onFailure { throwable -> Log.w(TAG, "no vibrator available", throwable) }
            .getOrNull()
    }

    /**
     * Plays the pattern for a severity.
     *
     * Never throws: an absent or refused vibrator is a degraded alert, not a
     * failed one.
     *
     * @param severity Urgency being announced.
     */
    fun vibrate(severity: Severity) {
        val pattern = patternFor(severity) ?: return
        val device = vibrator ?: return
        if (!device.hasVibrator()) return

        runCatching {
            device.vibrate(VibrationEffect.createWaveform(pattern, NO_REPEAT))
        }.onFailure { throwable ->
            Log.w(TAG, "vibration failed", throwable)
        }
    }

    /** Stops any pattern in progress. */
    fun cancel() {
        runCatching { vibrator?.cancel() }
            .onFailure { throwable -> Log.w(TAG, "vibration cancel failed", throwable) }
    }

    private fun resolveVibrator(): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(VibratorManager::class.java)
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }

    /**
     * Timings in wait/vibrate pairs.
     *
     * Critical is three long pulses — long enough to feel through a pocket and
     * distinct from any system haptic. High is a short double tap. Below that,
     * nothing: motion triggers on a curtain, and a phone buzzing at curtains is a
     * phone that gets silenced entirely.
     */
    private fun patternFor(severity: Severity): LongArray? = when {
        severity.isAtLeast(Severity.CRITICAL) ->
            longArrayOf(0, 400, 150, 400, 150, 400)

        severity.isAtLeast(Severity.HIGH) ->
            longArrayOf(0, 120, 90, 120)

        else -> null
    }

    private companion object {
        const val TAG = "AlertVibrator"

        /** `-1` means play the waveform once rather than looping it. */
        const val NO_REPEAT = -1
    }
}
