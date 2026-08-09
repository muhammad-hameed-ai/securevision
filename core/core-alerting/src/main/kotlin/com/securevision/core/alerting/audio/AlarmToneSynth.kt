package com.securevision.core.alerting.audio

import com.securevision.core.model.Severity
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.roundToInt

/**
 * Generates the alarm tones as raw PCM.
 *
 * Nothing here touches Android. That is the point: the tone is arithmetic, so it
 * can be tested on the JVM for pitch, envelope and clipping, while the class that
 * pushes bytes at an `AudioTrack` stays thin enough to need no tests of its own.
 *
 * Synthesized rather than bundled as an asset — no audio file ships with the app —
 * and synthesized rather than played through `ToneGenerator`, which offers a fixed
 * menu of system tones and no control over the two-tone alternation that makes an
 * alarm read as an alarm rather than a notification chime.
 */
object AlarmToneSynth {

    /** Samples per second. 44.1 kHz is the one rate every device supports. */
    const val SAMPLE_RATE = 44_100

    /**
     * Builds one cycle of the tone for a severity.
     *
     * For [Severity.CRITICAL] this is a single two-tone sweep intended to be looped
     * continuously until silenced; for [Severity.HIGH] it is a complete one-shot
     * chime. Below `HIGH` there is no tone at all and this returns an empty array —
     * motion fires on any movement in a static scene, and a phone that chirps at a
     * curtain teaches its owner to ignore it.
     *
     * @param severity Urgency being announced.
     * @return Signed 16-bit mono samples, or an empty array when silence is correct.
     */
    fun cycleFor(severity: Severity): ShortArray = when {
        severity.isAtLeast(Severity.CRITICAL) -> twoTone(
            firstHz = CRITICAL_LOW_HZ,
            secondHz = CRITICAL_HIGH_HZ,
            toneMillis = CRITICAL_TONE_MILLIS,
            gapMillis = CRITICAL_GAP_MILLIS,
        )

        severity.isAtLeast(Severity.HIGH) -> twoTone(
            firstHz = HIGH_LOW_HZ,
            secondHz = HIGH_HIGH_HZ,
            toneMillis = HIGH_TONE_MILLIS,
            gapMillis = HIGH_GAP_MILLIS,
        )

        else -> ShortArray(0)
    }

    /** Whether a severity loops until silenced, as opposed to playing once. */
    fun repeats(severity: Severity): Boolean = severity.isAtLeast(Severity.CRITICAL)

    /**
     * Two alternating pitches separated by a gap.
     *
     * The alternation is what distinguishes an alarm from a notification: a steady
     * pitch reads as a beep and is easy to tune out, whereas a changing one keeps
     * re-attracting attention.
     */
    private fun twoTone(
        firstHz: Double,
        secondHz: Double,
        toneMillis: Int,
        gapMillis: Int,
    ): ShortArray {
        val toneSamples = samplesFor(toneMillis)
        val gapSamples = samplesFor(gapMillis)
        val output = ShortArray(toneSamples * 2 + gapSamples * 2)

        renderTone(output, offset = 0, count = toneSamples, hz = firstHz)
        // Gap left as zeroes.
        renderTone(output, offset = toneSamples + gapSamples, count = toneSamples, hz = secondHz)

        return output
    }

    /**
     * Writes one windowed sine burst.
     *
     * The envelope is not decoration. A sine that starts and stops at full
     * amplitude has a discontinuity at each edge, which the speaker reproduces as
     * an audible click — and a click on every repeat of a looping alarm is what
     * makes cheap alarms sound broken.
     */
    private fun renderTone(target: ShortArray, offset: Int, count: Int, hz: Double) {
        if (count <= 0) return

        val rampSamples = min(samplesFor(RAMP_MILLIS), count / 2)
        val angularStep = 2.0 * PI * hz / SAMPLE_RATE

        for (index in 0 until count) {
            val envelope = when {
                rampSamples == 0 -> 1.0
                index < rampSamples -> index.toDouble() / rampSamples
                index >= count - rampSamples -> (count - index).toDouble() / rampSamples
                else -> 1.0
            }

            val value = sin(angularStep * index) * envelope * AMPLITUDE
            target[offset + index] = value.roundToInt().toShort()
        }
    }

    private fun samplesFor(millis: Int) = SAMPLE_RATE * millis / 1_000

    /**
     * Peak amplitude, held below `Short.MAX_VALUE`.
     *
     * Headroom rather than maximum loudness: rounding a full-scale sine can land
     * one sample above the range and wrap to a large negative value, which is
     * heard as a crack rather than a louder tone.
     */
    private const val AMPLITUDE = 26_000.0

    /** Fade in and out at each edge, long enough to kill the click. */
    private const val RAMP_MILLIS = 5

    private const val CRITICAL_LOW_HZ = 740.0
    private const val CRITICAL_HIGH_HZ = 988.0
    private const val CRITICAL_TONE_MILLIS = 180
    private const val CRITICAL_GAP_MILLIS = 60

    private const val HIGH_LOW_HZ = 587.0
    private const val HIGH_HIGH_HZ = 784.0
    private const val HIGH_TONE_MILLIS = 120
    private const val HIGH_GAP_MILLIS = 40
}
