package com.securevision.core.data.database

import androidx.room.Room
import app.cash.turbine.test
import com.securevision.core.data.database.dao.AlertDao
import com.securevision.core.data.database.dao.DetectionEventDao
import com.securevision.core.data.database.dao.EnrolledProfileDao
import com.securevision.core.data.database.entity.AlertEntity
import com.securevision.core.data.database.entity.DetectionEventEntity
import com.securevision.core.data.database.entity.EnrolledProfileEntity
import com.securevision.core.model.AlertType
import com.securevision.core.model.EnrolledProfile
import com.securevision.core.model.Severity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Exercises the DAOs against a real in-memory SQLite database.
 *
 * Runs under Robolectric rather than as an instrumented test so it executes on
 * every `gradlew build` and gates every commit, instead of only running when a
 * device happens to be attached.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SecureVisionDatabaseTest {

    private lateinit var database: SecureVisionDatabase
    private lateinit var profileDao: EnrolledProfileDao
    private lateinit var alertDao: AlertDao
    private lateinit var eventDao: DetectionEventDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            SecureVisionDatabase::class.java,
        ).allowMainThreadQueries().build()

        profileDao = database.enrolledProfileDao()
        alertDao = database.alertDao()
        eventDao = database.detectionEventDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // --- Enrolled profiles ---------------------------------------------------

    @Test
    fun `inserted profile is emitted by getAll`() = runTest {
        profileDao.insert(profile(id = "p1", name = "Ayesha"))

        profileDao.getAll().test {
            val rows = awaitItem()
            assertEquals(1, rows.size)
            assertEquals("Ayesha", rows.first().name)
        }
    }

    @Test
    fun `embedding survives a real database write and read`() = runTest {
        val embedding = FloatArray(EnrolledProfile.SHIPPED_MODEL_EMBEDDING_SIZE) { it * 0.0013f - 0.3f }
        profileDao.insert(profile(id = "p1", embedding = embedding))

        val stored = profileDao.getById("p1")

        // Through the type converter, SQLite's BLOB column, and back — bit-exact.
        assertArrayEquals(embedding, stored!!.embedding, 0f)
    }

    @Test
    fun `getAll orders newest enrolment first`() = runTest {
        profileDao.insert(profile(id = "old", name = "Old", createdAt = 1_000L))
        profileDao.insert(profile(id = "new", name = "New", createdAt = 2_000L))

        profileDao.getAll().test {
            assertEquals(listOf("New", "Old"), awaitItem().map(EnrolledProfileEntity::name))
        }
    }

    @Test
    fun `search matches a substring case-insensitively`() = runTest {
        profileDao.insert(profile(id = "p1", name = "Ayesha Khan"))
        profileDao.insert(profile(id = "p2", name = "Bilal Ahmed"))

        profileDao.search("khan").test {
            assertEquals(listOf("Ayesha Khan"), awaitItem().map(EnrolledProfileEntity::name))
        }
    }

    @Test
    fun `delete removes the row`() = runTest {
        profileDao.insert(profile(id = "p1"))

        profileDao.delete("p1")

        assertNull(profileDao.getById("p1"))
    }

    @Test
    fun `countAll tracks inserts without loading rows`() = runTest {
        profileDao.countAll().test {
            assertEquals(0, awaitItem())

            profileDao.insert(profile(id = "p1"))
            assertEquals(1, awaitItem())

            profileDao.insert(profile(id = "p2"))
            assertEquals(2, awaitItem())
        }
    }

    // --- Alerts --------------------------------------------------------------

    @Test
    fun `unread count reacts to markAllRead`() = runTest {
        alertDao.insert(alert(id = "a1", isRead = false))
        alertDao.insert(alert(id = "a2", isRead = false))

        alertDao.getUnreadCount().test {
            assertEquals(2, awaitItem())

            alertDao.markAllRead()

            assertEquals(0, awaitItem())
        }
    }

    @Test
    fun `getByType filters on the enum column`() = runTest {
        alertDao.insert(alert(id = "a1", type = AlertType.WEAPON))
        alertDao.insert(alert(id = "a2", type = AlertType.MOTION))

        alertDao.getByType(AlertType.WEAPON).test {
            val rows = awaitItem()
            assertEquals(1, rows.size)
            assertEquals("a1", rows.first().id)
        }
    }

    @Test
    fun `getRecent bounds the result and keeps newest first`() = runTest {
        repeat(times = 8) { index ->
            alertDao.insert(alert(id = "a$index", timestamp = index.toLong()))
        }

        alertDao.getRecent(limit = 3).test {
            assertEquals(listOf("a7", "a6", "a5"), awaitItem().map(AlertEntity::id))
        }
    }

    @Test
    fun `deleteOlderThan keeps the row exactly on the boundary`() = runTest {
        alertDao.insert(alert(id = "older", timestamp = 999L))
        alertDao.insert(alert(id = "boundary", timestamp = 1_000L))
        alertDao.insert(alert(id = "newer", timestamp = 1_001L))

        val deleted = alertDao.deleteOlderThan(timestamp = 1_000L)

        assertEquals(1, deleted)
        alertDao.getAll().test {
            assertEquals(setOf("newer", "boundary"), awaitItem().map(AlertEntity::id).toSet())
        }
    }

    // --- Detection events ----------------------------------------------------

    @Test
    fun `event count tracks inserts`() = runTest {
        eventDao.countAll().test {
            assertEquals(0, awaitItem())

            eventDao.insert(
                DetectionEventEntity(
                    id = "e1",
                    type = AlertType.MOTION,
                    label = "Movement",
                    confidence = 0.5f,
                    cameraFacing = "back",
                    timestamp = 1_000L,
                ),
            )

            assertEquals(1, awaitItem())
        }
    }

    // --- Fixtures ------------------------------------------------------------

    private fun profile(
        id: String,
        name: String = "Person $id",
        embedding: FloatArray = FloatArray(EnrolledProfile.SHIPPED_MODEL_EMBEDDING_SIZE) { 0.01f },
        createdAt: Long = 1_000L,
    ) = EnrolledProfileEntity(
        id = id,
        name = name,
        age = 30,
        photoUri = "file:///profiles/$id.jpg",
        embedding = embedding,
        isWatchlisted = false,
        createdAt = createdAt,
    )

    private fun alert(
        id: String,
        type: AlertType = AlertType.UNKNOWN_PERSON,
        isRead: Boolean = false,
        timestamp: Long = 1_000L,
    ) = AlertEntity(
        id = id,
        type = type,
        severity = Severity.HIGH,
        confidence = 0.9f,
        cameraFacing = "front",
        snapshotUri = null,
        hasBeard = null,
        hasMask = null,
        timestamp = timestamp,
        isRead = isRead,
    )
}
