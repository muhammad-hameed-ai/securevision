package com.securevision.core.data.database.migration

import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Verifies `MIGRATION_1_2` against a real v1 database.
 *
 * This is the test that stands between a schema bump and a wiped device.
 * `DatabaseModule` has no destructive fallback, so a migration that does not
 * produce exactly the schema Room expects fails on open — for the operator, on
 * the launch after an update, with their enrolled profiles inaccessible.
 *
 * A v1 database is built by hand rather than via `MigrationTestHelper`, which
 * needs instrumentation. The Phase 2 tables are populated first so the test can
 * prove the migration is additive and does not disturb existing rows.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MigrationTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var helper: SupportSQLiteOpenHelper
    private lateinit var database: SupportSQLiteDatabase

    @Before
    fun setUp() {
        val databaseFile = File(temporaryFolder.root, "migration-test.db")

        helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(RuntimeEnvironment.getApplication())
                .name(databaseFile.absolutePath)
                .callback(V1Schema)
                .build(),
        )
        database = helper.writableDatabase
    }

    @After
    fun tearDown() {
        helper.close()
    }

    @Test
    fun `migration creates the account table`() {
        Migrations.MIGRATION_1_2.migrate(database)

        assertTrue("user_accounts table missing", database.hasTable("user_accounts"))
    }

    @Test
    fun `migration creates the unique username index under Room's expected name`() {
        Migrations.MIGRATION_1_2.migrate(database)

        // Room validates the live schema against the generated one on open, and a
        // differently named index fails that check with a confusing error.
        assertTrue(
            "index_user_accounts_username missing",
            database.hasIndex("index_user_accounts_username"),
        )
    }

    @Test
    fun `the new table accepts an account row with every column`() {
        Migrations.MIGRATION_1_2.migrate(database)

        database.execSQL(
            """
            INSERT INTO user_accounts
                (uid, username, full_name, cnic, password_hash, recovery_code_hash, created_at)
            VALUES ('uid-1', 'hameed', 'Muhammad Hameed', '4210112345671', 'hash', 'code', 1)
            """.trimIndent(),
        )

        assertEquals(1, database.countRows("user_accounts"))
    }

    @Test
    fun `migration is additive and leaves existing data intact`() {
        database.execSQL(
            "INSERT INTO enrolled_profiles " +
                "(id, name, age, photo_uri, embedding, is_watchlisted, created_at) " +
                "VALUES ('p1', 'Ayesha', 30, 'file:///p1.jpg', X'00000000', 0, 1)",
        )
        database.execSQL(
            "INSERT INTO recordings (id, file_path, duration_ms, thumbnail_uri, created_at) " +
                "VALUES ('r1', '/clip.mp4', 1000, NULL, 1)",
        )

        Migrations.MIGRATION_1_2.migrate(database)

        assertEquals(1, database.countRows("enrolled_profiles"))
        assertEquals(1, database.countRows("recordings"))
        assertEquals(0, database.countRows("user_accounts"))
    }

    @Test
    fun `migration is idempotent`() {
        Migrations.MIGRATION_1_2.migrate(database)

        // IF NOT EXISTS throughout, so a retried upgrade cannot fail halfway.
        Migrations.MIGRATION_1_2.migrate(database)

        assertTrue(database.hasTable("user_accounts"))
    }

    private fun SupportSQLiteDatabase.hasTable(name: String): Boolean =
        query("SELECT name FROM sqlite_master WHERE type='table' AND name='$name'")
            .use { it.count > 0 }

    private fun SupportSQLiteDatabase.hasIndex(name: String): Boolean =
        query("SELECT name FROM sqlite_master WHERE type='index' AND name='$name'")
            .use { it.count > 0 }

    private fun SupportSQLiteDatabase.countRows(table: String): Int =
        query("SELECT COUNT(*) FROM $table").use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }

    /** The Phase 2 schema, exactly as shipped at version 1. */
    private object V1Schema : SupportSQLiteOpenHelper.Callback(1) {

        override fun onCreate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `enrolled_profiles` (
                    `id` TEXT NOT NULL, `name` TEXT NOT NULL, `age` INTEGER NOT NULL,
                    `photo_uri` TEXT NOT NULL, `embedding` BLOB NOT NULL,
                    `is_watchlisted` INTEGER NOT NULL, `created_at` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `alerts` (
                    `id` TEXT NOT NULL, `type` TEXT NOT NULL, `severity` TEXT NOT NULL,
                    `confidence` REAL NOT NULL, `camera_facing` TEXT NOT NULL,
                    `snapshot_uri` TEXT, `has_beard` INTEGER, `has_mask` INTEGER,
                    `timestamp` INTEGER NOT NULL, `is_read` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `detection_events` (
                    `id` TEXT NOT NULL, `type` TEXT NOT NULL, `label` TEXT NOT NULL,
                    `confidence` REAL NOT NULL, `camera_facing` TEXT NOT NULL,
                    `timestamp` INTEGER NOT NULL, PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `recordings` (
                    `id` TEXT NOT NULL, `file_path` TEXT NOT NULL,
                    `duration_ms` INTEGER NOT NULL, `thumbnail_uri` TEXT,
                    `created_at` INTEGER NOT NULL, PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
        }

        override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
            // The test drives migrations explicitly.
        }
    }
}
