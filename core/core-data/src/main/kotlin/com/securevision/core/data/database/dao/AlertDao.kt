package com.securevision.core.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.securevision.core.data.database.entity.AlertEntity
import com.securevision.core.model.AlertType
import kotlinx.coroutines.flow.Flow

/** Reads and writes for the alerts table. */
@Dao
interface AlertDao {

    /** Inserts or replaces an alert, keyed on its id. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(alert: AlertEntity)

    /** All alerts, newest first. */
    @Query("SELECT * FROM alerts ORDER BY timestamp DESC")
    fun getAll(): Flow<List<AlertEntity>>

    /**
     * Alerts of one category, newest first.
     *
     * @param type Category to filter by. Bound through the enum type converter.
     */
    @Query("SELECT * FROM alerts WHERE type = :type ORDER BY timestamp DESC")
    fun getByType(type: AlertType): Flow<List<AlertEntity>>

    /**
     * A bounded window of the newest alerts, for the dashboard preview.
     *
     * @param limit Maximum rows to emit.
     */
    @Query("SELECT * FROM alerts ORDER BY timestamp DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<AlertEntity>>

    /** Live count of unread alerts. */
    @Query("SELECT COUNT(*) FROM alerts WHERE is_read = 0")
    fun getUnreadCount(): Flow<Int>

    /** Marks every unread alert as read. */
    @Query("UPDATE alerts SET is_read = 1 WHERE is_read = 0")
    suspend fun markAllRead()

    /**
     * Retention pruning.
     *
     * @param timestamp Epoch milliseconds; rows strictly older than this are removed.
     * @return How many rows were deleted.
     */
    @Query("DELETE FROM alerts WHERE timestamp < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long): Int
}
