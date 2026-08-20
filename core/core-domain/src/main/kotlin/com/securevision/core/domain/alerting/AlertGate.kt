package com.securevision.core.domain.alerting

import com.securevision.core.common.Constants
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The single de-duplication guard for every alert in the app.
 *
 * There is exactly one of these. Persistence, the alarm and notifications all sit
 * behind the same [claim], so a suppressed alert is suppressed everywhere rather
 * than being written quietly while the phone stays silent — or, worse, the phone
 * sounding for an alert nobody recorded.
 *
 * It lives in the domain layer rather than in a ViewModel because the live screen
 * is not the only thing that will ever raise an alert. Anything added later that
 * alerts without going through that screen inherits this guard by construction,
 * which is the only way "one guard" survives contact with a growing codebase.
 *
 * Two independent conditions must both pass:
 *
 * 1. **A key alerts at most once.** A stranger held in view for ten seconds is one
 *    alert, not thirty.
 * 2. **Two alerts of the same kind never fall inside the same window.** This is
 *    what stops a detector that keeps reassigning tracking ids from walking
 *    straight past the first condition with a fresh key every frame.
 *
 * The window is per *kind*, not global. A single shared timestamp would let a
 * face alert swallow a weapon alert three seconds later — the most severe event
 * the app can produce, dropped because of an unrelated one. Per kind is also what
 * [Constants.Alerting.DUPLICATE_ALERT_WINDOW_MILLIS] is documented to mean:
 * minimum spacing between repeated alerts *of the same kind*.
 *
 * Safe to call from any thread; detectors run concurrently.
 */
@Singleton
class AlertGate @Inject constructor() {

    private val claimedKeys = mutableSetOf<String>()
    private val lastAlertAtByKind = mutableMapOf<String, Long>()

    /**
     * Attempts to claim the right to raise one alert.
     *
     * @param key `kind:identifier` — the tracking id for a face, the class name
     *   for a weapon, which has no tracking id. The prefix selects the window.
     * @param now Frame timestamp, epoch milliseconds UTC.
     * @return `true` when the caller may raise the alert. A `false` means it was
     *   de-duplicated, not that anything failed.
     */
    @Synchronized
    fun claim(key: String, now: Long): Boolean {
        if (key in claimedKeys) return false

        val kind = key.substringBefore(KEY_SEPARATOR)

        // Zero, not a sentinel: `now - Long.MIN_VALUE` overflows to a negative
        // number and would suppress the very first alert of every kind.
        val lastOfKind = lastAlertAtByKind[kind] ?: 0L
        if (now - lastOfKind < Constants.Alerting.DUPLICATE_ALERT_WINDOW_MILLIS) return false

        claimedKeys += key
        lastAlertAtByKind[kind] = now
        return true
    }

    /**
     * Forgets every claim.
     *
     * Called when the frame source changes — a camera flip restarts tracking ids,
     * so a retained claim would silence a genuinely new person who happens to be
     * assigned an id the previous lens had already used.
     */
    @Synchronized
    fun reset() {
        claimedKeys.clear()
        lastAlertAtByKind.clear()
    }

    companion object {

        /** Splits an alert key into its kind and its identifier. */
        const val KEY_SEPARATOR = ':'

        /** Key for a face, which de-duplicates per tracked person. */
        fun faceKey(trackingId: Int): String = "face$KEY_SEPARATOR$trackingId"

        /** Key for a weapon, which de-duplicates per class — weapons have no id. */
        fun weaponKey(weaponType: String): String = "weapon$KEY_SEPARATOR$weaponType"

        /**
         * Key for a recognised person, de-duplicated per profile.
         *
         * A separate kind from [faceKey] deliberately: a quiet known-person log
         * entry must not consume the window that would otherwise let a genuine
         * stranger alert through.
         */
        fun knownKey(profileId: String): String = "known$KEY_SEPARATOR$profileId"

        /** Key for motion, which has one bucket: the scene either moved or did not. */
        fun motionKey(): String = "motion$KEY_SEPARATOR"
    }
}
