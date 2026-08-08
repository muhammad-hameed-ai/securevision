package com.securevision.core.model

/**
 * User-adjustable behaviour of the detection pipeline and its alerting, as
 * exposed by the Settings screen and persisted on-device.
 *
 * The defaults declared here are the shipped defaults: this is the single place
 * that answers "what does SecureVision do before anyone changes anything?".
 *
 * @property alarmOnUnknownFace Sound the alarm while an unrecognised face is in
 *   frame.
 * @property alarmOnWeapon Sound the critical alarm on a weapon detection.
 * @property motionDetectionEnabled Raise alerts for movement in a static scene.
 * @property faceMatchThreshold Minimum cosine similarity for a face to count as
 *   a match, in `0f..1f`.
 * @property faceMatchMargin Minimum gap the best match must hold over the runner
 *   up before an identity is committed. Rejecting ambiguous matches is what
 *   prevents confident misidentification between similar-looking people.
 * @property votingFrameCount Number of consecutive frames that must agree before
 *   a tracked face leaves [MatchStatus.PROCESSING].
 * @property recordingEnabled Write overlay-burned video while the live screen is
 *   open.
 * @property notificationsEnabled Master switch for system notifications.
 * @property retentionDays How long alerts and events are kept before pruning.
 * @property useDarkTheme `null` follows the system setting.
 */
data class AppSettings(
    val alarmOnUnknownFace: Boolean = true,
    val alarmOnWeapon: Boolean = true,
    val motionDetectionEnabled: Boolean = true,
    val faceMatchThreshold: Float = DEFAULT_FACE_MATCH_THRESHOLD,
    val faceMatchMargin: Float = DEFAULT_FACE_MATCH_MARGIN,
    val votingFrameCount: Int = DEFAULT_VOTING_FRAME_COUNT,
    val recordingEnabled: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val retentionDays: Int = DEFAULT_RETENTION_DAYS,
    val useDarkTheme: Boolean? = null,
) {
    companion object {
        /**
         * Cosine similarity above which two aligned FaceNet-512 embeddings are
         * considered the same person.
         */
        const val DEFAULT_FACE_MATCH_THRESHOLD: Float = 0.70f

        /** Required lead of the best match over the second-best match. */
        const val DEFAULT_FACE_MATCH_MARGIN: Float = 0.08f

        /** Consecutive agreeing frames required to commit to an identity. */
        const val DEFAULT_VOTING_FRAME_COUNT: Int = 5

        /** Days of alert and event history retained on-device. */
        const val DEFAULT_RETENTION_DAYS: Int = 30
    }
}
