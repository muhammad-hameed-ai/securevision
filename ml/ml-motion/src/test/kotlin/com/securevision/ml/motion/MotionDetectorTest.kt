package com.securevision.ml.motion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The ratio maths and the debounce.
 *
 * The debounce is the half that matters operationally: without it, a person
 * crossing a room at a 350 ms cadence produces roughly three alerts a second,
 * and an alerts list nobody will read again.
 */
class MotionDetectorTest {

    private val detector = MotionDetector()

    @Test
    fun `the first frame reports nothing, having nothing to compare against`() {
        val result = detector.compare(grid(uniform = 100), THRESHOLD, nowMillis = 0L)

        assertFalse(result.hasMotion)
        assertFalse(result.isNewEvent)
        assertEquals(0f, result.intensity, TOLERANCE)
    }

    @Test
    fun `identical frames report no motion`() {
        detector.compare(grid(uniform = 100), THRESHOLD, nowMillis = 0L)

        val result = detector.compare(grid(uniform = 100), THRESHOLD, nowMillis = 400L)

        assertFalse(result.hasMotion)
        assertEquals(0f, result.intensity, TOLERANCE)
    }

    @Test
    fun `a change below the per-cell threshold is treated as sensor noise`() {
        detector.compare(grid(uniform = 100), THRESHOLD, nowMillis = 0L)

        // Every cell shifts, but only by less than PER_CELL_THRESHOLD.
        val result = detector.compare(
            grid(uniform = 100 + MotionDetector.PER_CELL_THRESHOLD),
            THRESHOLD,
            nowMillis = 400L,
        )

        assertFalse("noise-level change must not read as motion", result.hasMotion)
        assertEquals(0f, result.intensity, TOLERANCE)
    }

    @Test
    fun `a change above the per-cell threshold counts`() {
        detector.compare(grid(uniform = 100), THRESHOLD, nowMillis = 0L)

        val result = detector.compare(
            grid(uniform = 100 + MotionDetector.PER_CELL_THRESHOLD + 1),
            THRESHOLD,
            nowMillis = 400L,
        )

        assertTrue(result.hasMotion)
        assertEquals(1f, result.intensity, TOLERANCE)
    }

    @Test
    fun `intensity is the fraction of cells that changed`() {
        val size = 4
        detector.compare(grid(uniform = 0, size = size), THRESHOLD, nowMillis = 0L)

        // 4 of 16 cells change by well over the per-cell threshold.
        val changed = IntArray(size * size) { index -> if (index < 4) 200 else 0 }
        val result = detector.compare(
            LuminanceGrid(changed, size),
            intensityThreshold = 0.1f,
            nowMillis = 400L,
        )

        assertEquals(0.25f, result.intensity, TOLERANCE)
    }

    @Test
    fun `a ratio below the intensity threshold is not motion`() {
        val size = 4
        detector.compare(grid(uniform = 0, size = size), THRESHOLD, nowMillis = 0L)

        val changed = IntArray(size * size) { index -> if (index < 4) 200 else 0 }
        val result = detector.compare(
            LuminanceGrid(changed, size),
            intensityThreshold = 0.5f,
            nowMillis = 400L,
        )

        assertEquals(0.25f, result.intensity, TOLERANCE)
        assertFalse(result.hasMotion)
    }

    @Test
    fun `continuous motion is one event, not a stream`() {
        detector.compare(grid(uniform = 0), THRESHOLD, nowMillis = 0L)

        val first = detector.compare(grid(uniform = 200), THRESHOLD, nowMillis = 400L)
        val second = detector.compare(grid(uniform = 0), THRESHOLD, nowMillis = 800L)
        val third = detector.compare(grid(uniform = 200), THRESHOLD, nowMillis = 1_200L)

        assertTrue("first crossing starts the event", first.isNewEvent)
        assertTrue(second.hasMotion)
        assertFalse("still the same event", second.isNewEvent)
        assertFalse("still the same event", third.isNewEvent)
    }

    @Test
    fun `a new event can start once the scene has been quiet long enough`() {
        detector.compare(grid(uniform = 0), THRESHOLD, nowMillis = 0L)
        assertTrue(detector.compare(grid(uniform = 200), THRESHOLD, nowMillis = 400L).isNewEvent)

        // Still, for longer than the quiet period.
        detector.compare(grid(uniform = 200), THRESHOLD, nowMillis = 1_000L)
        detector.compare(
            grid(uniform = 200),
            THRESHOLD,
            nowMillis = 1_000L + MotionDetector.QUIET_PERIOD_MILLIS + 1,
        )

        val reArmed = detector.compare(
            grid(uniform = 0),
            THRESHOLD,
            nowMillis = 1_000L + MotionDetector.QUIET_PERIOD_MILLIS + 500,
        )

        assertTrue("should re-arm after the quiet period", reArmed.isNewEvent)
    }

    @Test
    fun `a brief pause does not split one event into two`() {
        detector.compare(grid(uniform = 0), THRESHOLD, nowMillis = 0L)
        detector.compare(grid(uniform = 200), THRESHOLD, nowMillis = 400L)

        // Still for well under the quiet period — someone pausing mid-stride.
        detector.compare(grid(uniform = 200), THRESHOLD, nowMillis = 800L)

        val resumed = detector.compare(grid(uniform = 0), THRESHOLD, nowMillis = 1_200L)

        assertTrue(resumed.hasMotion)
        assertFalse("a pause must not start a second event", resumed.isNewEvent)
    }

    @Test
    fun `reset makes the next frame a first frame again`() {
        detector.compare(grid(uniform = 0), THRESHOLD, nowMillis = 0L)
        detector.compare(grid(uniform = 200), THRESHOLD, nowMillis = 400L)

        detector.reset()
        val afterReset = detector.compare(grid(uniform = 0), THRESHOLD, nowMillis = 800L)

        // A camera flip must not read as one enormous motion event.
        assertFalse(afterReset.hasMotion)
        assertEquals(0f, afterReset.intensity, TOLERANCE)
    }

    @Test
    fun `a grid size change is handled as a fresh start, not an error`() {
        detector.compare(grid(uniform = 0, size = 8), THRESHOLD, nowMillis = 0L)

        val resized = detector.compare(grid(uniform = 200, size = 4), THRESHOLD, nowMillis = 400L)

        assertFalse(resized.hasMotion)
    }

    @Test
    fun `changedRatioAgainst rejects mismatched grids`() {
        val small = grid(uniform = 0, size = 4)
        val large = grid(uniform = 0, size = 8)

        val failure = runCatching { small.changedRatioAgainst(large, 25) }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
    }

    private fun grid(uniform: Int, size: Int = 8) =
        LuminanceGrid(IntArray(size * size) { uniform }, size)

    private companion object {
        const val THRESHOLD = 0.02f
        const val TOLERANCE = 1e-4f
    }
}
