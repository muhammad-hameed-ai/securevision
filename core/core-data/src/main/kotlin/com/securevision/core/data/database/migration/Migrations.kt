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

    /** Every migration, in order, for the database builder. */
    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2)
}
