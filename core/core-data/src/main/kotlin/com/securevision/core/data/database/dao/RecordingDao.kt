package com.securevision.core.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.securevision.core.data.database.entity.RecordingEntity
import kotlinx.coroutines.flow.Flow

/** Reads and writes for the recordings metadata table. */
@Dao
interface RecordingDao {

    /** Inserts or replaces a recording, keyed on its id. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recording: RecordingEntity)

    /** All recordings, newest first. */
    @Query("SELECT * FROM recordings ORDER BY created_at DESC")
    fun getAll(): Flow<List<RecordingEntity>>

    /**
     * Looked up before deletion so the repository knows which file to remove
     * from internal storage.
     *
     * @param id Recording identifier.
     */
    @Query("SELECT * FROM recordings WHERE id = :id")
    suspend fun getById(id: String): RecordingEntity?

    /** @param id Recording identifier to remove. */
    @Query("DELETE FROM recordings WHERE id = :id")
    suspend fun delete(id: String)
}
