package com.securevision.core.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.securevision.core.model.AlertType
import com.securevision.core.model.Severity

/**
 * Room row for a user-visible alert.
 *
 * Indexed on the three columns the DAO actually filters and sorts by. Adding an
 * index now is free; adding one later is a migration.
 *
 * @property id Locally generated stable identifier.
 * @property type Category of event that raised the alert.
 * @property severity How urgent it is.
 * @property confidence Detector confidence in `0f..1f`.
 * @property label Detector subject — weapon class or recognised person's name.
 * @property cameraFacing Which camera produced it.
 * @property snapshotUri `file://` URI of the captured frame, or `null`.
 * @property hasBeard Beard attribute when a face was analysed, `null` otherwise.
 * @property hasMask Mask attribute when a face was analysed, `null` otherwise.
 * @property timestamp When the event occurred, epoch milliseconds UTC.
 * @property isRead Whether the user has opened the alerts list since.
 */
@Entity(
    tableName = AlertEntity.TABLE_NAME,
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["type"]),
        Index(value = ["is_read"]),
    ],
)
data class AlertEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "type")
    val type: AlertType,

    @ColumnInfo(name = "severity")
    val severity: Severity,

    /** Added in schema v4; rows written before it migrate to an empty string. */
    @ColumnInfo(name = "label", defaultValue = "")
    val label: String,

    @ColumnInfo(name = "confidence")
    val confidence: Float,

    /** Added in schema v5; rows written before it keep `null` = not assessed. */
    @ColumnInfo(name = "estimated_age")
    val estimatedAge: Int?,

    @ColumnInfo(name = "camera_facing")
    val cameraFacing: String,

    @ColumnInfo(name = "snapshot_uri")
    val snapshotUri: String?,

    @ColumnInfo(name = "has_beard")
    val hasBeard: Boolean?,

    @ColumnInfo(name = "has_mask")
    val hasMask: Boolean?,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    @ColumnInfo(name = "is_read")
    val isRead: Boolean,
) {
    companion object {
        const val TABLE_NAME = "alerts"
    }
}
