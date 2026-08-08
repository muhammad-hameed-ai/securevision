package com.securevision.core.data.mapper

import com.securevision.core.data.database.entity.RecordingEntity
import com.securevision.core.model.Recording

/** Converts a stored row into its domain model. */
fun RecordingEntity.toDomain(): Recording = Recording(
    id = id,
    filePath = filePath,
    durationMs = durationMs,
    thumbnailUri = thumbnailUri,
    createdAt = createdAt,
)

/** Converts a domain model into a storable row. */
fun Recording.toEntity(): RecordingEntity = RecordingEntity(
    id = id,
    filePath = filePath,
    durationMs = durationMs,
    thumbnailUri = thumbnailUri,
    createdAt = createdAt,
)

/** Converts a list of rows into domain models, preserving order. */
@JvmName("recordingEntitiesToDomain")
fun List<RecordingEntity>.toDomain(): List<Recording> = map(RecordingEntity::toDomain)
