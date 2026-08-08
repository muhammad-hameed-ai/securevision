package com.securevision.feature.recordings

/**
 * Navigation identity of the recordings feature.
 *
 * Phase 6 adds the recordings gallery and a Media3 player for clips captured
 * with detection overlays burned in. Recordings stay in the app's internal
 * storage and are never written to shared media.
 */
object RecordingsRoutes {

    /** Gallery of recorded clips. */
    const val RECORDINGS = "recordings"

    /** Full-screen player for one clip. */
    const val PLAYER = "recordings/player"

    /** Argument key carrying the recording identifier into [PLAYER]. */
    const val ARG_RECORDING_ID = "recordingId"
}
