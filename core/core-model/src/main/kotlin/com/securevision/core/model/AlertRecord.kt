package com.securevision.core.model

/**
 * A persisted, user-visible alert — one row in the Alerts screen and the payload
 * behind one notification.
 *
 * @property id Locally generated stable identifier.
 * @property type What kind of event raised this alert.
 * @property severity How urgent it is.
 * @property label The detector's subject — a weapon class, or the name of a
 *   recognised person. Empty when the type says everything there is to say, as
 *   for motion. Stored rather than derived: "Recognised: Ayesha" cannot be
 *   reconstructed from the category alone.
 * @property confidence Detector confidence in `0f..1f`.
 * @property cameraFacing Which camera produced it, e.g. `"front"` or `"back"`.
 * @property snapshotUri URI of the captured frame shown inside the notification,
 *   or `null` when no snapshot was taken (motion alerts, typically).
 * @property estimatedAge Estimated age in years when an age model assessed the
 *   face, `null` otherwise. `null` means not assessed and must never render as a
 *   number or a band — the same discipline as [hasBeard].
 * @property hasBeard Beard attribute when a face was analysed, `null` otherwise.
 * @property hasMask Mask attribute when a face was analysed, `null` otherwise.
 * @property timestamp When the event occurred, epoch milliseconds UTC.
 * @property isRead Whether the user has since opened the alerts list.
 */
data class AlertRecord(
    val id: String,
    val type: AlertType,
    val severity: Severity,
    val label: String = "",
    val confidence: Float,
    val cameraFacing: String,
    val snapshotUri: String?,
    val estimatedAge: Int? = null,
    val hasBeard: Boolean?,
    val hasMask: Boolean?,
    val timestamp: Long,
    val isRead: Boolean,
)
