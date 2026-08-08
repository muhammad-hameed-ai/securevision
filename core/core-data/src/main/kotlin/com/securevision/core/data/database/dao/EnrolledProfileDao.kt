package com.securevision.core.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.securevision.core.data.database.entity.EnrolledProfileEntity
import kotlinx.coroutines.flow.Flow

/** Reads and writes for the enrolled person profiles table. */
@Dao
interface EnrolledProfileDao {

    /** Inserts or replaces a profile, keyed on its id. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: EnrolledProfileEntity)

    /** All profiles, newest enrolment first. */
    @Query("SELECT * FROM enrolled_profiles ORDER BY created_at DESC")
    fun getAll(): Flow<List<EnrolledProfileEntity>>

    /**
     * @param id Profile identifier.
     * @return The row, or `null` if no profile has that id.
     */
    @Query("SELECT * FROM enrolled_profiles WHERE id = :id")
    suspend fun getById(id: String): EnrolledProfileEntity?

    /** @param id Profile identifier to remove. */
    @Query("DELETE FROM enrolled_profiles WHERE id = :id")
    suspend fun delete(id: String)

    /**
     * Name search, ordered alphabetically.
     *
     * SQLite's `LIKE` is already case-insensitive for ASCII, so no `COLLATE` is
     * needed for the common case.
     *
     * @param query Substring to match against the name.
     */
    @Query("SELECT * FROM enrolled_profiles WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun search(query: String): Flow<List<EnrolledProfileEntity>>

    /**
     * Live profile count.
     *
     * A dedicated `COUNT(*)` so the dashboard does not read a 512-float embedding
     * per person just to show a number.
     */
    @Query("SELECT COUNT(*) FROM enrolled_profiles")
    fun countAll(): Flow<Int>
}
