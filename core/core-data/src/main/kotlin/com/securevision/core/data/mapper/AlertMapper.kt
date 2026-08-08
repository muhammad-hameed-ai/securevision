package com.securevision.core.data.mapper

import com.securevision.core.data.database.entity.AlertEntity
import com.securevision.core.model.AlertRecord

/**
 * Converts a stored row into its domain model.
 *
 * Nullable columns stay null rather than being coerced to a default: `hasBeard =
 * null` means "no face was analysed", which is a different statement from
 * `hasBeard = false`, and a notification must not claim the latter when it only
 * knows the former.
 */
fun AlertEntity.toDomain(): AlertRecord = AlertRecord(
    id = id,
    type = type,
    severity = severity,
    confidence = confidence,
    cameraFacing = cameraFacing,
    snapshotUri = snapshotUri,
    hasBeard = hasBeard,
    hasMask = hasMask,
    timestamp = timestamp,
    isRead = isRead,
)

/** Converts a domain model into a storable row. */
fun AlertRecord.toEntity(): AlertEntity = AlertEntity(
    id = id,
    type = type,
    severity = severity,
    confidence = confidence,
    cameraFacing = cameraFacing,
    snapshotUri = snapshotUri,
    hasBeard = hasBeard,
    hasMask = hasMask,
    timestamp = timestamp,
    isRead = isRead,
)

/** Converts a list of rows into domain models, preserving order. */
@JvmName("alertEntitiesToDomain")
fun List<AlertEntity>.toDomain(): List<AlertRecord> = map(AlertEntity::toDomain)
