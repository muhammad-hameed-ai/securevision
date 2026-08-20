package com.securevision.core.model

/**
 * A video clip captured from the live camera, stored in the app's internal
 * storage.
 *
 * **The clip holds the camera feed only — detection boxes are not burned in.**
 * CameraX records the sensor stream, while the overlay is a Compose layer above
 * the preview; there is no API that composites one into the other. Doing it
 * properly needs a custom OpenGL pipeline feeding `MediaCodec`, which is future
 * work. The gallery says this on screen rather than letting anyone assume the
 * boxes were captured.
 *
 * Silent by design: audio would require `RECORD_AUDIO`, and this app ships with
 * exactly three permissions.
 *
 * @property id Locally generated stable identifier.
 * @property filePath Absolute path within internal storage. Recordings are not
 *   written to shared media storage — they stay private to the app.
 * @property durationMs Clip length in milliseconds.
 * @property thumbnailUri URI of the generated poster frame, or `null` if
 *   thumbnail generation has not completed.
 * @property createdAt Recording start time, epoch milliseconds UTC.
 */
data class Recording(
    val id: String,
    val filePath: String,
    val durationMs: Long,
    val thumbnailUri: String?,
    val createdAt: Long,
)
