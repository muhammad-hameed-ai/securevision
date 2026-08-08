package com.securevision.core.model

/**
 * A single detected face in one analysed frame, together with its recognition
 * verdict. This is what the live overlay renders.
 *
 * @property trackingId Identifier that stays stable across frames for the same
 *   face, supplied by the detector's tracker. It is the key that multi-frame
 *   voting accumulates evidence against.
 * @property boundingBox Face location in normalised frame coordinates.
 * @property matchStatus Whether this face is known, unknown, or still being
 *   resolved.
 * @property profileId Matched [EnrolledProfile.id], or `null` unless
 *   [matchStatus] is [MatchStatus.KNOWN].
 * @property profileName Matched profile's display name, or `null` unless
 *   [matchStatus] is [MatchStatus.KNOWN].
 * @property confidence Cosine similarity of the winning match, in `0f..1f`.
 */
data class DetectionResult(
    val trackingId: Int,
    val boundingBox: BoundingBox,
    val matchStatus: MatchStatus,
    val profileId: String?,
    val profileName: String?,
    val confidence: Float,
)
