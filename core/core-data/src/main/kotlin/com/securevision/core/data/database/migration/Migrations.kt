package com.securevision.core.data.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Schema migrations for [com.securevision.core.data.database.SecureVisionDatabase].
 *
 * `DatabaseModule` deliberately does not set `fallbackToDestructiveMigration`, so
 * every version bump needs an entry here. On an offline-only app that is not
 * pedantry: destructive fallback would wipe the operator's account and every
 * enrolled person profile, none of which exists anywhere else.
 */
object Migrations {

    /**
     * v1 → v2: adds the app-login account table.
     *
     * Purely additive. The Phase 2 tables — profiles, alerts, events, recordings
     * — are not touched, so an existing install keeps all of its data.
     *
     * The index name matches Room's own convention (`index_<table>_<column>`)
     * because Room validates the live schema against the generated one on open,
     * and a differently named index fails that check.
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `user_accounts` (
                    `uid` TEXT NOT NULL,
                    `username` TEXT NOT NULL,
                    `full_name` TEXT NOT NULL,
                    `cnic` TEXT NOT NULL,
                    `password_hash` TEXT NOT NULL,
                    `recovery_code_hash` TEXT NOT NULL,
                    `created_at` INTEGER NOT NULL,
                    PRIMARY KEY(`uid`)
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_user_accounts_username` " +
                    "ON `user_accounts` (`username`)",
            )
        }
    }

    /**
     * v2 → v3: adds the operator access-level classification to profiles.
     *
     * Additive, with a `STANDARD` default, so every existing enrolment keeps its
     * embedding and its photo and simply acquires the default label. Nobody has to
     * re-enrol a face — which matters more here than in most apps, because the
     * embedding exists nowhere but this table.
     *
     * `NOT NULL DEFAULT 'STANDARD'` must match the entity's `defaultValue`
     * exactly: Room compares the live schema against the generated one when the
     * database opens, and a default declared on one side only fails that check.
     */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE `enrolled_profiles` " +
                    "ADD COLUMN `access_level` TEXT NOT NULL DEFAULT 'STANDARD'",
            )
        }
    }

    /**
     * v3 → v4: adds the detector subject to alerts.
     *
     * Additive, defaulting to an empty string. Existing alerts keep everything
     * they had and simply carry no subject — correct, since the app never
     * recorded one for them and inventing one would be a fabricated audit entry.
     */
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `alerts` ADD COLUMN `label` TEXT NOT NULL DEFAULT ''")
        }
    }

    /**
     * v4 → v5: adds the estimated age to alerts.
     *
     * Nullable with no default, deliberately. Every existing alert keeps `null`,
     * which the app renders as "unknown" — the honest answer, because no age
     * model examined those faces. A default of zero would have every historical
     * intruder recorded as a newborn.
     */
    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `alerts` ADD COLUMN `estimated_age` INTEGER DEFAULT NULL")
        }
    }

    /** Every migration, in order, for the database builder. */
    val ALL: Array<Migration> =
        arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
}
