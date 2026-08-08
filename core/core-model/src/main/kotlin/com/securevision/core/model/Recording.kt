package com.securevision.core.model

/**
 * A video clip captured with detection overlays burned in, stored in the app's
 * internal storage.
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
