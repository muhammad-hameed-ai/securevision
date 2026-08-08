package com.securevision.core.model

/**
 * User-adjustable behaviour of the detection pipeline and its alerting,
 * persisted on-device.
 *
 * The defaults declared here are the shipped defaults: this is the single place
 * that answers "what does SecureVision do before anyone changes anything?".
 *
 * @property confidenceThreshold Minimum cosine similarity for a face to count as
 *   a match, in `0f..1f`.
 * @property matchMargin Minimum lead the best match must hold over the
 *   runner-up before an identity is committed. Rejecting ambiguous matches is
 *   what prevents confidently confusing two similar-looking people.
 * @property voteFrames Consecutive agreeing frames required before a tracked
 *   face leaves [MatchStatus.PROCESSING].
 * @property faceDetectionEnabled Detect and recognise faces.
 * @property objectDetectionEnabled Detect people and general objects.
 * @property weaponDetectionEnabled Detect weapons and raise a critical alarm.
 * @property motionDetectionEnabled Alert on movement in a static scene.
 * @property attributeAnalysisEnabled Infer age, gender and emotion. Off by
 *   default: it costs an extra inference pass per face, and only beard and mask
 *   are required by the notification copy.
 * @property alertSoundEnabled Play the alarm sound on qualifying alerts.
 * @property vibrationEnabled Vibrate on qualifying alerts.
 * @property pushNotificationsEnabled Post system notifications.
 * @property cameraResolution Capture resolution for the live camera.
 * @property dataRetentionDays How long alerts and events are kept before pruning.
 * @property darkMode Use the dark theme.
 */
data class AppSettings(
    val confidenceThreshold: Float = DEFAULT_CONFIDENCE_THRESHOLD,
    val matchMargin: Float = DEFAULT_MATCH_MARGIN,
    val voteFrames: Int = DEFAULT_VOTE_FRAMES,
    val faceDetectionEnabled: Boolean = true,
    val objectDetectionEnabled: Boolean = true,
    val weaponDetectionEnabled: Boolean = true,
    val motionDetectionEnabled: Boolean = true,
    val attributeAnalysisEnabled: Boolean = false,
    val alertSoundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val pushNotificationsEnabled: Boolean = true,
    val cameraResolution: CameraResolution = CameraResolution.HD_720,
    val dataRetentionDays: Int = DEFAULT_DATA_RETENTION_DAYS,
    val darkMode: Boolean = true,
) {
    companion object {

        /**
         * Cosine similarity above which two aligned FaceNet-512 embeddings are
         * considered the same person.
         */
        const val DEFAULT_CONFIDENCE_THRESHOLD: Float = 0.75f

        /** Required lead of the best match over the second-best match. */
        const val DEFAULT_MATCH_MARGIN: Float = 0.05f

        /** Consecutive agreeing frames required to commit to an identity. */
        const val DEFAULT_VOTE_FRAMES: Int = 3

        /** Days of alert and event history retained on-device. */
        const val DEFAULT_DATA_RETENTION_DAYS: Int = 30
    }
}
