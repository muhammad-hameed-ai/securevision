package com.securevision.core.domain.repository

import com.securevision.core.model.DetectionEvent
import kotlinx.coroutines.flow.Flow

/**
 * Append-only audit log of everything the detection pipeline observed, backing
 * the History screen.
 */
interface DetectionEventRepository {

    /**
     * Appends an event.
     *
     * @param event The detection to record.
     */
    suspend fun save(event: DetectionEvent)

    /** All events, newest first, re-emitted whenever the set changes. */
    fun getAll(): Flow<List<DetectionEvent>>

    /** Live total event count, shown on the dashboard. */
    fun countAll(): Flow<Int>
}
