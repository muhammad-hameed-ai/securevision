package com.securevision.ml.motion

import com.securevision.core.model.MotionResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Detects movement by comparing consecutive downscaled frames.
 *
 * Stateful: it holds the previous grid and the debounce clock. The debounce is
 * the part that matters operationally — without it, someone walking across a room
 * produces a motion event on every analysed frame, which at a 350 ms cadence is
 * roughly three alerts a second and an alerts list nobody will ever read again.
 *
 * Pure arithmetic with no Android dependency, so all of it is unit-testable.
 */
@Singleton
class MotionDetector @Inject constructor() {

    private var previous: LuminanceGrid? = null
    private var eventActive = false
    private var lastQuietAt = 0L

    /**
     * Compares a grid against the previous one.
     *
     * @param current Downscaled luminance for this frame.
     * @param intensityThreshold Changed-cell fraction that counts as motion.
     * @param nowMillis Current time, injectable so the debounce is testable
     *   without sleeping.
     * @return The result. The first call after [reset] always reports no motion,
     *   because there is nothing to compare against.
     */
    fun compare(
        current: LuminanceGrid,
        intensityThreshold: Float,
        nowMillis: Long = System.currentTimeMillis(),
    ): MotionResult {
        val baseline = previous
        previous = current

        if (baseline == null || baseline.size != current.size) {
            // First frame of a session, or the source changed size. Nothing to
            // compare, and reporting motion here would fire on every camera flip.
            lastQuietAt = nowMillis
            eventActive = false
            return MotionResult.NONE
        }

        val ratio = current.changedRatioAgainst(baseline, PER_CELL_THRESHOLD)
        val moving = ratio >= intensityThreshold

        if (!moving) {
            // Only re-arm once the scene has been quiet for the full cooldown, so
            // a subject pausing mid-stride does not split into two events.
            if (eventActive && nowMillis - lastQuietAt >= QUIET_PERIOD_MILLIS) {
                eventActive = false
            }
            if (!eventActive) lastQuietAt = nowMillis

            return MotionResult(hasMotion = false, intensity = ratio, isNewEvent = false)
        }

        val startsEvent = !eventActive
        if (startsEvent) {
            eventActive = true
        }
        lastQuietAt = nowMillis

        return MotionResult(hasMotion = true, intensity = ratio, isNewEvent = startsEvent)
    }

    /** Discards the previous frame and the debounce state. */
    fun reset() {
        previous = null
        eventActive = false
        lastQuietAt = 0L
    }

    companion object {
        /**
         * Per-cell luminance delta that counts as changed, `0..255`.
         *
         * Set above typical sensor noise. Lowering it makes a dim room register
         * constant motion; raising it misses a person in low contrast clothing.
         */
        const val PER_CELL_THRESHOLD = 25

        /**
         * How long the scene must stay still before a new event can start.
         *
         * Without it, a subject who pauses for one frame produces two alerts
         * instead of one.
         */
        const val QUIET_PERIOD_MILLIS = 3_000L
    }
}
