package com.securevision.core.model

/**
 * A lightweight, append-only log entry for the History screen.
 *
 * Distinct from [AlertRecord]: an alert is something the user was interrupted
 * for and can mark as read, whereas a detection event is the raw audit trail of
 * everything the pipeline saw. Every alert has a corresponding event; not every
 * event becomes an alert.
 *
 * @property id Locally generated stable identifier.
 * @property type Category of the detection.
 * @property label Human-readable subject, e.g. a matched profile name or an
 *   object class.
 * @property confidence Detector confidence in `0f..1f`.
 * @property cameraFacing Which camera produced it, e.g. `"front"` or `"back"`.
 * @property timestamp When the detection occurred, epoch milliseconds UTC.
 */
data class DetectionEvent(
    val id: String,
    val type: AlertType,
    val label: String,
    val confidence: Float,
    val cameraFacing: String,
    val timestamp: Long,
)
