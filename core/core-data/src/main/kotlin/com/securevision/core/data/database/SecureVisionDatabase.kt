package com.securevision.core.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.securevision.core.data.database.converter.EmbeddingConverter
import com.securevision.core.data.database.converter.EnumConverters
import com.securevision.core.data.database.dao.AlertDao
import com.securevision.core.data.database.dao.DetectionEventDao
import com.securevision.core.data.database.dao.EnrolledProfileDao
import com.securevision.core.data.database.dao.RecordingDao
import com.securevision.core.data.database.dao.UserAccountDao
import com.securevision.core.data.database.entity.AlertEntity
import com.securevision.core.data.database.entity.DetectionEventEntity
import com.securevision.core.data.database.entity.EnrolledProfileEntity
import com.securevision.core.data.database.entity.RecordingEntity
import com.securevision.core.data.database.entity.UserAccountEntity

/**
 * The on-device database. Every table here stays on the phone.
 *
 * Schemas are exported to `core-data/schemas` and committed, because they are the
 * input Room needs to generate and test migrations. Bumping [VERSION] without a
 * migration will crash on upgrade — deliberately, rather than silently dropping
 * a user's enrolled profiles.
 */
@Database(
    entities = [
        EnrolledProfileEntity::class,
        AlertEntity::class,
        DetectionEventEntity::class,
        RecordingEntity::class,
        UserAccountEntity::class,
    ],
    version = SecureVisionDatabase.VERSION,
    exportSchema = true,
)
@TypeConverters(EmbeddingConverter::class, EnumConverters::class)
abstract class SecureVisionDatabase : RoomDatabase() {

    /** Enrolled person profiles used for face recognition. */
    abstract fun enrolledProfileDao(): EnrolledProfileDao

    /** User-visible alerts. */
    abstract fun alertDao(): AlertDao

    /** The append-only detection audit trail. */
    abstract fun detectionEventDao(): DetectionEventDao

    /** Recorded clip metadata. */
    abstract fun recordingDao(): RecordingDao

    /** The single app-login account. */
    abstract fun userAccountDao(): UserAccountDao

    companion object {
        /**
         * Schema version. Increment only alongside a migration in
         * [com.securevision.core.data.database.migration.Migrations].
         *
         * v1 — Phase 2: profiles, alerts, detection events, recordings.
         * v2 — Phase 3: the app-login account.
         */
        const val VERSION = 2
    }
}
