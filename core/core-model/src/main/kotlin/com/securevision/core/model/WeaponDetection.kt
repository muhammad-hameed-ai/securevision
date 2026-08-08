package com.securevision.core.model

/**
 * A weapon located in one analysed frame. Rendered as an orange overlay box and
 * escalated to [Severity.CRITICAL].
 *
 * @property boundingBox Weapon location in normalised frame coordinates.
 * @property weaponType Class label reported by the detector, e.g. `"knife"`.
 * @property confidence Detector confidence in `0f..1f`.
 */
data class WeaponDetection(
    val boundingBox: BoundingBox,
    val weaponType: String,
    val confidence: Float,
)
