package com.securevision.core.domain.alerting

import com.securevision.core.model.AlertType
import com.securevision.core.model.FaceAttributes
import com.securevision.core.model.Severity

/**
 * One alert, described completely, before anything has been done about it.
 *
 * Built through the factories on [Companion] rather than constructed directly.
 * They are what decide that a weapon is [Severity.CRITICAL] and motion is
 * [Severity.LOW], and they build the de-duplication key next to the kind it
 * belongs to — so a key and a severity cannot drift apart the way they would if
 * each caller assembled its own.
 *
 * @property dedupKey Key for [AlertGate]; see [AlertGate.claim].
 * @property type Category, which selects the notification channel.
 * @property severity Urgency, which selects the alarm behaviour.
 * @property label Audit-trail subject, e.g. `"knife"`. Not user-facing: the
 *   History screen maps the type onto a localised string.
 * @property confidence Detector score in `0f..1f`.
 * @property cameraFacing Which camera saw it, `"front"` or `"back"`.
 * @property snapshotUri Captured still, or `null` when none was saved.
 * @property attributes Soft attributes. Defaults to
 *   [FaceAttributes.NOT_ASSESSED], whose fields are all `null` — which is what
 *   reaches the database, so an alert never claims "no beard" about a person no
 *   classifier examined.
 * @property timestamp When the event was confirmed, epoch milliseconds UTC.
 */
data class AlertRequest(
    val dedupKey: String,
    val type: AlertType,
    val severity: Severity,
    val label: String,
    val confidence: Float,
    val cameraFacing: String,
    val snapshotUri: String? = null,
    val attributes: FaceAttributes = FaceAttributes.NOT_ASSESSED,
    val timestamp: Long = System.currentTimeMillis(),
) {
    companion object {

        /**
         * A confirmed sighting of someone not enrolled.
         *
         * [Severity.HIGH]: worth interrupting for, but not the critical alarm —
         * an unrecognised face is frequently a guest, a courier or a reflection.
         *
         * @param trackingId Detector tracking id, which keys the de-duplication.
         * @param confidence Best similarity seen, even though it was rejected.
         * @param cameraFacing Which camera saw them.
         * @param snapshotUri Captured still, or `null`.
         * @param attributes Soft attributes, all `null` when nothing looked.
         * @param timestamp When the sighting was confirmed.
         */
        fun unknownPerson(
            trackingId: Int,
            confidence: Float,
            cameraFacing: String,
            snapshotUri: String? = null,
            attributes: FaceAttributes = FaceAttributes.NOT_ASSESSED,
            timestamp: Long = System.currentTimeMillis(),
        ) = AlertRequest(
            dedupKey = AlertGate.faceKey(trackingId),
            type = AlertType.UNKNOWN_PERSON,
            severity = Severity.HIGH,
            label = UNKNOWN_PERSON_LABEL,
            confidence = confidence,
            cameraFacing = cameraFacing,
            snapshotUri = snapshotUri,
            attributes = attributes,
            timestamp = timestamp,
        )

        /**
         * A detected weapon.
         *
         * Always [Severity.CRITICAL], never downgraded on a low score. The weapon
         * confidence threshold already sits higher than every other detector's,
         * precisely so that anything reaching here is worth interrupting for.
         *
         * @param weaponType Detector class label, e.g. `"knife"`.
         * @param confidence Detector score in `0f..1f`.
         * @param cameraFacing Which camera saw it.
         * @param snapshotUri Captured still, or `null`.
         * @param timestamp When it was confirmed.
         */
        fun weapon(
            weaponType: String,
            confidence: Float,
            cameraFacing: String,
            snapshotUri: String? = null,
            timestamp: Long = System.currentTimeMillis(),
        ) = AlertRequest(
            dedupKey = AlertGate.weaponKey(weaponType),
            type = AlertType.WEAPON,
            severity = Severity.CRITICAL,
            label = weaponType,
            confidence = confidence,
            cameraFacing = cameraFacing,
            snapshotUri = snapshotUri,
            // No face was analysed for a weapon; null, never false.
            attributes = FaceAttributes.NOT_ASSESSED,
            timestamp = timestamp,
        )

        /**
         * Movement in an otherwise static scene.
         *
         * [Severity.LOW] on purpose. Movement is the weakest signal this app
         * produces — a curtain, a pet, a change in light all trigger it — and
         * rating it higher would train the operator to dismiss the severity badge
         * entirely, which would then be dismissed on the weapon alert too.
         *
         * @param intensity Fraction of the frame that changed, in `0f..1f`.
         * @param cameraFacing Which camera saw it.
         * @param snapshotUri Captured still, or `null`.
         * @param timestamp When it was confirmed.
         */
        fun motion(
            intensity: Float,
            cameraFacing: String,
            snapshotUri: String? = null,
            timestamp: Long = System.currentTimeMillis(),
        ) = AlertRequest(
            dedupKey = AlertGate.motionKey(),
            type = AlertType.MOTION,
            severity = Severity.LOW,
            label = MOTION_LABEL,
            confidence = intensity,
            cameraFacing = cameraFacing,
            snapshotUri = snapshotUri,
            attributes = FaceAttributes.NOT_ASSESSED,
            timestamp = timestamp,
        )

        private const val UNKNOWN_PERSON_LABEL = "unknown_person"
        private const val MOTION_LABEL = "motion"
    }
}
