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

    /**
     * The most recent alerts, newest first.
     *
     * Bounded at the query rather than by taking from [getAll], so the dashboard
     * preview does not grow into a full table read as history accumulates.
     *
     * @param limit Maximum number of alerts to emit.
     */
    fun getRecent(limit: Int): Flow<List<AlertRecord>>

    /** Live count of unread alerts, for the navigation badge. */
    fun getUnreadCount(): Flow<Int>

    /** Marks every stored alert as read. */
    suspend fun markAllRead()

    /**
     * Looks up one alert.
     *
     * @param id Alert identifier.
     * @return The alert, or `null` if it is no longer stored.
     */
    suspend fun getById(id: String): AlertRecord?

    /**
     * Dismisses one alert.
     *
     * Undo is [save] with the same record rather than a soft-delete flag: the row
     * carries its own id and timestamp, so re-inserting it restores it exactly,
     * and no query anywhere has to learn to skip tombstones.
     *
     * @param id Alert identifier.
     */
    suspend fun delete(id: String)

    /**
     * Prunes old alerts as part of the retention policy.
     *
     * @param timestamp Epoch milliseconds; alerts strictly older than this are removed.
     * @return How many alerts were deleted.
     */
    suspend fun deleteOlderThan(timestamp: Long): Int
}
