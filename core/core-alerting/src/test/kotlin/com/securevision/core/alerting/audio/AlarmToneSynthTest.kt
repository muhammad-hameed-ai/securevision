package com.securevision.core.alerting.audio

import com.securevision.core.model.Severity
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The alarm tone as arithmetic.
 *
 * This is why the synth is a separate class from the player: pitch, envelope and
 * headroom are all checkable without a device, and the only thing left untested is
 * the handful of lines that hand a buffer to `AudioTrack`.
 */
class AlarmToneSynthTest {

    @Test
    fun `a critical alarm produces audio`() {
        val cycle = AlarmToneSynth.cycleFor(Severity.CRITICAL)

        assertTrue(cycle.isNotEmpty())
        assertTrue(cycle.any { sample -> sample != 0.toShort() })
    }

    @Test
    fun `a high alert produces audio`() {
        assertTrue(AlarmToneSynth.cycleFor(Severity.HIGH).isNotEmpty())
    }

    @Test
    fun `motion is silent`() {
        // A tone on LOW would have the phone chirping at a curtain, and an alarm
        // the user learns to ignore is worse than no alarm.
        assertTrue(AlarmToneSynth.cycleFor(Severity.LOW).isEmpty())
    }

    @Test
    fun `medium is silent`() {
        assertTrue(AlarmToneSynth.cycleFor(Severity.MEDIUM).isEmpty())
    }

    @Test
    fun `only the critical alarm repeats`() {
        assertTrue(AlarmToneSynth.repeats(Severity.CRITICAL))
        assertFalse(AlarmToneSynth.repeats(Severity.HIGH))
        assertFalse(AlarmToneSynth.repeats(Severity.LOW))
    }

    @Test
    fun `no sample clips`() {
        // A sample that rounds past Short.MAX_VALUE wraps to a large negative
        // number, which is heard as a crack rather than a louder tone.
        val cycle = AlarmToneSynth.cycleFor(Severity.CRITICAL)

        val peak = cycle.maxOf { sample -> abs(sample.toInt()) }
        assertTrue("peak $peak should leave headroom", peak < Short.MAX_VALUE)
    }

    @Test
    fun `the cycle starts and ends near silence`() {
        // Without the envelope each repeat of a looping alarm begins with a
        // discontinuity, which the speaker reproduces as an audible click.
        val cycle = AlarmToneSynth.cycleFor(Severity.CRITICAL)

        assertEquals(0, cycle.first().toInt())
        assertTrue(abs(cycle.last().toInt()) < QUIET_EDGE)
    }

    @Test
    fun `the critical cycle contains a gap between the two tones`() {
        val cycle = AlarmToneSynth.cycleFor(Severity.CRITICAL)

        // The alternation is what distinguishes an alarm from a beep, and the gap
        // is what makes the two pitches read as separate.
        val silentRun = longestSilentRun(cycle)
        assertTrue("expected a gap, longest silent run was $silentRun", silentRun > MIN_GAP_SAMPLES)
    }

    @Test
    fun `the two halves of the cycle differ in pitch`() {
        val cycle = AlarmToneSynth.cycleFor(Severity.CRITICAL)
        val half = cycle.size / 2

        val firstCrossings = zeroCrossings(cycle, 0, half)
        val secondCrossings = zeroCrossings(cycle, half, cycle.size)

        // Zero crossings scale with frequency, so a genuine pitch change shows up
        // here without needing an FFT.
        assertTrue(
            "expected different pitches, got $firstCrossings and $secondCrossings crossings",
            firstCrossings != secondCrossings,
        )
    }

    @Test
    fun `the critical alarm is longer than the high chime`() {
        // Deliberate: critical loops and should feel weightier per repeat.
        assertTrue(
            AlarmToneSynth.cycleFor(Severity.CRITICAL).size >
                AlarmToneSynth.cycleFor(Severity.HIGH).size,
        )
    }

    @Test
    fun `the sample rate is one every device supports`() {
        assertEquals(44_100, AlarmToneSynth.SAMPLE_RATE)
    }

    private fun longestSilentRun(samples: ShortArray): Int {
        var longest = 0
        var current = 0

        samples.forEach { sample ->
            if (sample.toInt() == 0) {
                current++
                if (current > longest) longest = current
            } else {
                current = 0
            }
        }
        return longest
    }

    private fun zeroCrossings(samples: ShortArray, from: Int, to: Int): Int {
        var crossings = 0
        for (index in from + 1 until to) {
            val previous = samples[index - 1].toInt()
            val current = samples[index].toInt()
            if (previous <= 0 && current > 0) crossings++
        }
        return crossings
    }

    private companion object {
        /** The ramp lands the final sample close to, but not exactly at, zero. */
        const val QUIET_EDGE = 2_000

        /** Well under the real gap, so the test does not encode the exact timing. */
        const val MIN_GAP_SAMPLES = 500
    }
}
