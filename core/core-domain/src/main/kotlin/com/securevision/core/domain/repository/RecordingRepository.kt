package com.securevision.core.domain.repository

import com.securevision.core.model.Recording
import kotlinx.coroutines.flow.Flow

/** On-device storage for recorded video clips and their metadata. */
interface RecordingRepository {

    /**
     * Registers a finished recording.
     *
     * @param recording Metadata for a clip already written to internal storage.
     */
    suspend fun save(recording: Recording)

    /** All recordings, newest first, re-emitted whenever the set changes. */
    fun getAll(): Flow<List<Recording>>

    /**
     * Removes a recording's metadata and deletes its file from internal storage.
     *
     * @param id The recording identifier.
     */
    suspend fun delete(id: String)
}
