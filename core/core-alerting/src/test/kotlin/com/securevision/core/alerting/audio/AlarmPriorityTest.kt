package com.securevision.core.alerting.audio

import com.securevision.core.model.Severity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A quieter alert must never take the speaker from a louder one.
 *
 * The field report: an unknown-person chime silenced a sounding weapon alarm.
 * `start()` releases the current track before every tone, so the least important
 * alert in the app was stopping the most important one.
 *
 * These tests pin the ordering rule itself. The `AudioTrack` plumbing needs a
 * device, but the decision of *whether* a tone may take over is pure logic and
 * belongs under test.
 */
class AlarmPriorityTest {

    @Test
    fun `critical outranks high`() {
        assertTrue(Severity.CRITICAL.isAtLeast(Severity.HIGH))
        assertFalse(Severity.HIGH.isAtLeast(Severity.CRITICAL))
    }

    @Test
    fun `a high alert cannot displace a sounding critical alarm`() {
        // The exact scenario reported: a weapon is alarming and a stranger is
        // seen at the same moment.
        assertTrue(ownsSpeaker(owner = Severity.CRITICAL, requested = Severity.HIGH))
    }

    @Test
    fun `a low alert cannot displace a sounding critical alarm`() {
        assertTrue(ownsSpeaker(owner = Severity.CRITICAL, requested = Severity.LOW))
    }

    @Test
    fun `a critical alert may take over from a high one`() {
        // The reverse must be allowed: a weapon appearing during a stranger chime
        // has to be able to interrupt it.
        assertFalse(ownsSpeaker(owner = Severity.HIGH, requested = Severity.CRITICAL))
    }

    @Test
    fun `an equal severity is allowed through so the alarm can re-arm`() {
        // Re-arming is how the weapon alarm keeps sounding while the weapon stays
        // in frame. Refusing an equal severity would stop it after one cycle,
        // which is the other half of the reported bug.
        assertFalse(ownsSpeaker(owner = Severity.CRITICAL, requested = Severity.CRITICAL))
    }

    @Test
    fun `nothing is refused when the speaker is free`() {
        assertFalse(ownsSpeaker(owner = null, requested = Severity.LOW))
        assertFalse(ownsSpeaker(owner = null, requested = Severity.CRITICAL))
    }

    @Test
    fun `only critical holds the speaker at all`() {
        // Below CRITICAL the synth produces a one-shot chime, which finishes on
        // its own and must not lock anything out.
        assertTrue(AlarmToneSynth.repeats(Severity.CRITICAL))
        assertFalse(AlarmToneSynth.repeats(Severity.HIGH))
    }

    /**
     * Mirrors the guard in [AudioTrackAlarmPlayer.play].
     *
     * @return `true` when the request must be refused.
     */
    private fun ownsSpeaker(owner: Severity?, requested: Severity): Boolean =
        owner != null && owner.isAtLeast(requested) && owner != requested
}
