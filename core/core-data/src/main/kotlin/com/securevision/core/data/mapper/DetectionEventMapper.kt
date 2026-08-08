package com.securevision.core.data.mapper

import com.securevision.core.data.database.entity.DetectionEventEntity
import com.securevision.core.model.DetectionEvent

/** Converts a stored row into its domain model. */
fun DetectionEventEntity.toDomain(): DetectionEvent = DetectionEvent(
    id = id,
    type = type,
    label = label,
    confidence = confidence,
    cameraFacing = cameraFacing,
    timestamp = timestamp,
)

/** Converts a domain model into a storable row. */
fun DetectionEvent.toEntity(): DetectionEventEntity = DetectionEventEntity(
    id = id,
    type = type,
    label = label,
    confidence = confidence,
    cameraFacing = cameraFacing,
    timestamp = timestamp,
)

/** Converts a list of rows into domain models, preserving order. */
@JvmName("detectionEventEntitiesToDomain")
fun List<DetectionEventEntity>.toDomain(): List<DetectionEvent> = map(DetectionEventEntity::toDomain)
