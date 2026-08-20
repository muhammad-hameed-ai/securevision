package com.securevision.feature.live.camera

import androidx.compose.runtime.Immutable

/**
 * What the recorder is doing, as the screen needs to see it.
 *
 * @property isRecording Whether capture is running.
 * @property elapsedMillis How long the current clip has been running.
 * @property isAvailable Whether video capture bound successfully at all. Some
 *   devices cannot bind preview, analysis and video together; when that happens
 *   the record button is disabled rather than failing on tap.
 * @property detectionPausedWhileRecording Set when the camera had to give up the
 *   analysis stream to fit video capture. The screen says so, because a security
 *   app that quietly stops detecting is worse than one that cannot record.
 */
@Immutable
data class VideoRecorderState(
    val isRecording: Boolean = false,
    val elapsedMillis: Long = 0L,
    val isAvailable: Boolean = false,
    val detectionPausedWhileRecording: Boolean = false,
)
