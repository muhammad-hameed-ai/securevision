package com.securevision.core.domain.alerting

import com.securevision.core.model.AlertRecord

/**
 * Posts an alert to the system notification shade.
 *
 * Implementations must **never throw**: they report the outcome instead, because
 * "the user refused the permission" is an ordinary state this app has to keep
 * working in, not an error. The alert is already recorded by the time this is
 * called.
 */
interface AlertNotifier {

    /**
     * Posts one notification.
     *
     * @param alert The alert to announce. Its snapshot is attached when present,
     *   and its `label` names the subject — "Knife detected" rather than the
     *   much weaker "Weapon detected" on a lock screen.
     * @return What actually happened, so the caller can explain a silent phone
     *   rather than leaving the user to wonder.
     */
    suspend fun post(alert: AlertRecord): NotificationOutcome
}

/**
 * The result of trying to post a notification.
 *
 * Distinguishes "refused" from "broken" because only one of them is worth telling
 * the user about, and the remedy differs.
 */
enum class NotificationOutcome {

    /** Delivered to the shade. */
    POSTED,

    /** The user has not granted `POST_NOTIFICATIONS`; the alert was still recorded. */
    PERMISSION_DENIED,

    /** The platform rejected it. Unexpected, and worth surfacing as a fault. */
    FAILED,
}
