package com.securevision.core.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room row for an enrolled person profile.
 *
 * On-device only. This table is excluded from cloud backup and device transfer
 * at the manifest level because [embedding] is biometric data.
 *
 * @property id Locally generated stable identifier.
 * @property name Display name.
 * @property age Age in years, captured at enrolment.
 * @property photoUri `file://` URI of the enrolment photo in internal storage.
 * @property embedding L2-normalised FaceNet-512 vector, stored as a BLOB.
 * @property isWatchlisted Whether a sighting should be escalated.
 * @property createdAt Enrolment time, epoch milliseconds UTC.
 */
@Entity(
    tableName = EnrolledProfileEntity.TABLE_NAME,
    indices = [Index(value = ["name"])],
)
class EnrolledProfileEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "age")
    val age: Int,

    @ColumnInfo(name = "photo_uri")
    val photoUri: String,

    @ColumnInfo(name = "embedding", typeAffinity = ColumnInfo.BLOB)
    val embedding: FloatArray,

    @ColumnInfo(name = "is_watchlisted")
    val isWatchlisted: Boolean,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,
) {

    /**
     * Structural equality with [embedding] compared by content.
     *
     * Hand-written for the same reason as the domain model: a `data class`
     * compares `FloatArray` by reference, so two rows holding identical
     * embeddings would be unequal and every mapper test would be meaningless.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EnrolledProfileEntity) return false

        return id == other.id &&
            name == other.name &&
            age == other.age &&
            photoUri == other.photoUri &&
            embedding.contentEquals(other.embedding) &&
            isWatchlisted == other.isWatchlisted &&
            createdAt == other.createdAt
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + age
        result = 31 * result + photoUri.hashCode()
        result = 31 * result + embedding.contentHashCode()
        result = 31 * result + isWatchlisted.hashCode()
        result = 31 * result + createdAt.hashCode()
        return result
    }

    override fun toString(): String =
        "EnrolledProfileEntity(id=$id, name=$name, age=$age, photoUri=$photoUri, " +
            "embedding=${embedding.size} dims, isWatchlisted=$isWatchlisted, createdAt=$createdAt)"

    companion object {
        const val TABLE_NAME = "enrolled_profiles"
    }
}
