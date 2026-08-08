package com.securevision.core.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room row for a recorded clip.
 *
 * Holds metadata only. The clip itself lives in internal storage at [filePath];
 * deleting a row must therefore also delete that file, which is the repository's
 * job, not the DAO's.
 *
 * @property id Locally generated stable identifier.
 * @property filePath Absolute path inside the app's internal storage.
 * @property durationMs Clip length in milliseconds.
 * @property thumbnailUri `file://` URI of the poster frame, or `null`.
 * @property createdAt Recording start time, epoch milliseconds UTC.
 */
@Entity(
    tableName = RecordingEntity.TABLE_NAME,
    indices = [Index(value = ["created_at"])],
)
data class RecordingEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "file_path")
    val filePath: String,

    @ColumnInfo(name = "duration_ms")
    val durationMs: Long,

    @ColumnInfo(name = "thumbnail_uri")
    val thumbnailUri: String?,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,
) {
    companion object {
        const val TABLE_NAME = "recordings"
    }
}
