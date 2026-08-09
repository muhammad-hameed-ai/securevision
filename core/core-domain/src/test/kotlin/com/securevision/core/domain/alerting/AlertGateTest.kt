package com.securevision.core.domain.alerting

import com.securevision.core.common.Constants
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The de-duplication contract, pinned in the layer that owns it.
 *
 * These are the assertions that stop Phase 5a's on-device behaviour regressing:
 * one alert per person, and one detector never silencing another.
 */
class AlertGateTest {

    private val gate = AlertGate()

    private val window = Constants.Alerting.DUPLICATE_ALERT_WINDOW_MILLIS

    /** A realistic clock. Zero would hide sign and overflow mistakes. */
    private val start = 1_700_000_000_000L

    @Test
    fun `the first alert of a kind always passes`() {
        assertTrue(gate.claim(AlertGate.faceKey(1), start))
    }

    @Test
    fun `a key claims only once`() {
        gate.claim(AlertGate.faceKey(7), start)

        // Same person, long after the window: still one alert, because the key
        // has already been used.
        assertFalse(gate.claim(AlertGate.faceKey(7), start + window * 10))
    }

    @Test
    fun `a new key of the same kind is blocked inside the window`() {
        gate.claim(AlertGate.faceKey(1), start)

        // This is the case a per-key guard alone would miss: a detector that keeps
        // reassigning tracking ids would otherwise alert on every frame.
        assertFalse(gate.claim(AlertGate.faceKey(2), start + window / 2))
    }

    @Test
    fun `a new key of the same kind passes once the window has elapsed`() {
        gate.claim(AlertGate.faceKey(1), start)

        assertTrue(gate.claim(AlertGate.faceKey(2), start + window))
    }

    @Test
    fun `a weapon alert is not blocked by a face alert in the same instant`() {
        gate.claim(AlertGate.faceKey(1), start)

        // The reason the window is per kind. A shared timestamp would drop the
        // most severe event the app can produce because of an unrelated one.
        assertTrue(gate.claim(AlertGate.weaponKey("pistol"), start))
    }

    @Test
    fun `motion is not blocked by a weapon alert in the same instant`() {
        gate.claim(AlertGate.weaponKey("knife"), start)

        assertTrue(gate.claim(AlertGate.motionKey(), start))
    }

    @Test
    fun `two weapon classes still share one window`() {
        gate.claim(AlertGate.weaponKey("pistol"), start)

        // Same kind, so the window applies: a model flickering between two labels
        // for one object must not produce two alarms.
        assertFalse(gate.claim(AlertGate.weaponKey("rifle"), start + window / 2))
    }

    @Test
    fun `reset allows a previously claimed key again`() {
        gate.claim(AlertGate.faceKey(7), start)

        gate.reset()

        // Tracking ids restart on a camera flip, so id 7 after a reset is a
        // different person and deserves its own alert.
        assertTrue(gate.claim(AlertGate.faceKey(7), start + 1))
    }

    @Test
    fun `keys carry their kind as a prefix`() {
        // The prefix is what selects the window, so it is part of the contract
        // rather than an implementation detail of the string.
        assertTrue(AlertGate.faceKey(3).startsWith("face${AlertGate.KEY_SEPARATOR}"))
        assertTrue(AlertGate.weaponKey("knife").startsWith("weapon${AlertGate.KEY_SEPARATOR}"))
        assertTrue(AlertGate.motionKey().startsWith("motion${AlertGate.KEY_SEPARATOR}"))
    }
}
