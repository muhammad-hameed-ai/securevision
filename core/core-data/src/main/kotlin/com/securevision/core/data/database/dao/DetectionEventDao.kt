package com.securevision.core.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.securevision.core.data.database.entity.DetectionEventEntity
import kotlinx.coroutines.flow.Flow

/** Reads and writes for the append-only detection event log. */
@Dao
interface DetectionEventDao {

    /** Appends an event. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: DetectionEventEntity)

    /** All events, newest first. */
    @Query("SELECT * FROM detection_events ORDER BY timestamp DESC")
    fun getAll(): Flow<List<DetectionEventEntity>>

    /** Live total event count, shown on the dashboard. */
    @Query("SELECT COUNT(*) FROM detection_events")
    fun countAll(): Flow<Int>

    /**
     * Retention pruning.
     *
     * @param timestamp Epoch milliseconds; rows strictly older than this are removed.
     * @return How many rows were deleted.
     */
    @Query("DELETE FROM detection_events WHERE timestamp < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long): Int
}
