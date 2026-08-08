package com.securevision.core.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.securevision.core.model.AlertType

/**
 * Room row for the append-only detection audit trail behind the History screen.
 *
 * @property id Locally generated stable identifier.
 * @property type Category of the detection.
 * @property label Human-readable subject: a matched profile name or object class.
 * @property confidence Detector confidence in `0f..1f`.
 * @property cameraFacing Which camera produced it.
 * @property timestamp When the detection occurred, epoch milliseconds UTC.
 */
@Entity(
    tableName = DetectionEventEntity.TABLE_NAME,
    indices = [Index(value = ["timestamp"])],
)
data class DetectionEventEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "type")
    val type: AlertType,

    @ColumnInfo(name = "label")
    val label: String,

    @ColumnInfo(name = "confidence")
    val confidence: Float,

    @ColumnInfo(name = "camera_facing")
    val cameraFacing: String,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,
) {
    companion object {
        const val TABLE_NAME = "detection_events"
    }
}
