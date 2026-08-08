package com.securevision.core.data.mapper

import com.securevision.core.data.database.entity.EnrolledProfileEntity
import com.securevision.core.model.EnrolledProfile

/**
 * Converts a stored row into its domain model.
 *
 * The embedding array is passed through by reference rather than copied: it is
 * freshly decoded from the BLOB by the type converter on every read, so no
 * caller can be holding the same instance.
 */
fun EnrolledProfileEntity.toDomain(): EnrolledProfile = EnrolledProfile(
    id = id,
    name = name,
    age = age,
    photoUri = photoUri,
    embedding = embedding,
    isWatchlisted = isWatchlisted,
    createdAt = createdAt,
)

/** Converts a domain model into a storable row. */
fun EnrolledProfile.toEntity(): EnrolledProfileEntity = EnrolledProfileEntity(
    id = id,
    name = name,
    age = age,
    photoUri = photoUri,
    embedding = embedding,
    isWatchlisted = isWatchlisted,
    createdAt = createdAt,
)

/** Converts a list of rows into domain models, preserving order. */
fun List<EnrolledProfileEntity>.toDomain(): List<EnrolledProfile> = map(EnrolledProfileEntity::toDomain)
