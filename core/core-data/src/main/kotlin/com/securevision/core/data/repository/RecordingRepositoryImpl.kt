package com.securevision.core.data.repository

import com.securevision.core.common.dispatcher.DispatcherProvider
import com.securevision.core.data.database.dao.RecordingDao
import com.securevision.core.data.mapper.toDomain
import com.securevision.core.data.mapper.toEntity
import com.securevision.core.data.storage.InternalStorageManager
import com.securevision.core.domain.repository.RecordingRepository
import com.securevision.core.model.Recording
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Room-backed recording metadata, paired with the clip files themselves.
 *
 * @property dao Recording table access.
 * @property storageManager Owns the clip and thumbnail files.
 * @property dispatcherProvider Supplies the IO dispatcher for one-shot writes.
 */
@Singleton
class RecordingRepositoryImpl @Inject constructor(
    private val dao: RecordingDao,
    private val storageManager: InternalStorageManager,
    private val dispatcherProvider: DispatcherProvider,
) : RecordingRepository {

    override suspend fun save(recording: Recording) = withContext(dispatcherProvider.io) {
        dao.insert(recording.toEntity())
    }

    override fun getAll(): Flow<List<Recording>> = dao.getAll().map { it.toDomain() }

    /**
     * Removes the row along with the clip and its thumbnail.
     *
     * Deleting the row without the file would leak storage that nothing in the app
     * can ever reach again, since the path only existed in this table.
     */
    override suspend fun delete(id: String) = withContext(dispatcherProvider.io) {
        dao.getById(id)?.let { existing ->
            storageManager.deleteFile(existing.filePath)
            existing.thumbnailUri?.let { storageManager.deleteFile(it) }
        }
        dao.delete(id)
    }
}
