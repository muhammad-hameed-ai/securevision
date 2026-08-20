package com.securevision.core.data.repository

import androidx.room.Room
import com.securevision.core.data.database.SecureVisionDatabase
import com.securevision.core.data.database.entity.EnrolledProfileEntity
import com.securevision.core.model.AccessLevel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * The Dashboard figure and the People list must never disagree.
 *
 * Reported from the field: the Dashboard showed "0 enrolled profiles" while a
 * profile existed and recognition was working. The count is now derived from the
 * same stream the list renders, and these tests hold the two together against a
 * real database rather than a mock — a mock would have happily agreed with itself
 * no matter which source each side read.
 */
@RunWith(RobolectricTestRunner::class)
class ProfileCountConsistencyTest {

    private lateinit var database: SecureVisionDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            SecureVisionDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `an empty database counts zero`() = runTest {
        assertEquals(0, database.enrolledProfileDao().countAll().first())
    }

    @Test
    fun `the count matches the list after an insert`() = runTest {
        val dao = database.enrolledProfileDao()
        dao.insert(profile("1"))

        assertEquals(dao.getAll().first().size, dao.countAll().first())
        assertEquals(1, dao.countAll().first())
    }

    @Test
    fun `the count follows further inserts`() = runTest {
        val dao = database.enrolledProfileDao()

        dao.insert(profile("1"))
        dao.insert(profile("2"))

        assertEquals(2, dao.countAll().first())
        assertEquals(dao.getAll().first().size, dao.countAll().first())
    }

    @Test
    fun `the count drops back after a delete`() = runTest {
        val dao = database.enrolledProfileDao()
        dao.insert(profile("1"))

        dao.delete("1")

        assertEquals(0, dao.countAll().first())
        assertEquals(dao.getAll().first().size, dao.countAll().first())
    }

    @Test
    fun `re-enrolling the same person does not double the count`() = runTest {
        val dao = database.enrolledProfileDao()
        dao.insert(profile("1", name = "Ayesha"))

        // Re-enrolment keeps the id and replaces the row.
        dao.insert(profile("1", name = "Ayesha", age = 31))

        assertEquals(1, dao.countAll().first())
    }

    private fun profile(
        id: String,
        name: String = "Person $id",
        age: Int = 30,
    ) = EnrolledProfileEntity(
        id = id,
        name = name,
        age = age,
        photoUri = "file:///profiles/$id.jpg",
        embedding = FloatArray(512) { 0.01f },
        accessLevel = AccessLevel.STANDARD,
        isWatchlisted = false,
        createdAt = 1_700_000_000_000L,
    )
}
