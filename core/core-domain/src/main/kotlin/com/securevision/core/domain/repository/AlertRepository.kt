package com.securevision.core.domain.repository

import com.securevision.core.model.AlertRecord
import com.securevision.core.model.AlertType
import kotlinx.coroutines.flow.Flow

/** On-device storage for user-visible alerts. */
interface AlertRepository {

    /**
     * Persists an alert.
     *
     * @param alert The alert to store.
     */
    suspend fun save(alert: AlertRecord)

    /** All alerts, newest first, re-emitted whenever the set changes. */
    fun getAll(): Flow<List<AlertRecord>>

    /**
     * Alerts of a single category, newest first.
     *
     * @param type Category to filter by.
     */
    fun getByType(type: AlertType): Flow<List<AlertRecord>>

    /** Live count of unread alerts, for the navigation badge. */
    fun getUnreadCount(): Flow<Int>

    /** Marks every stored alert as read. */
    suspend fun markAllRead()

    /**
     * Prunes old alerts as part of the retention policy.
     *
     * @param timestamp Epoch milliseconds; alerts strictly older than this are removed.
     * @return How many alerts were deleted.
     */
    suspend fun deleteOlderThan(timestamp: Long): Int
}
