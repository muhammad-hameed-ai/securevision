package com.securevision.feature.recordings

/**
 * Navigation identity of the recordings feature.
 *
 * Recordings stay in the app's internal storage and are never written to shared
 * media. Clips hold the camera feed only — detection boxes are drawn on screen
 * and are not composited into the video.
 */
object RecordingsRoutes {

    /** Gallery of recorded clips. */
    const val RECORDINGS = "recordings"

    /** Argument key carrying the recording identifier into [PLAYER]. */
    const val ARG_RECORDING_ID = "recordingId"

    /** Full-screen player for one clip. */
    const val PLAYER = "recordings/player/{$ARG_RECORDING_ID}"

    /**
     * Builds the player route for one clip.
     *
     * @param recordingId Which clip to play.
     */
    fun player(recordingId: String): String = "recordings/player/$recordingId"
}
