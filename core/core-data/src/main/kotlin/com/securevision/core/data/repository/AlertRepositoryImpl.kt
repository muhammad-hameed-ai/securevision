package com.securevision.core.data.repository

import com.securevision.core.common.dispatcher.DispatcherProvider
import com.securevision.core.data.database.dao.AlertDao
import com.securevision.core.data.mapper.toDomain
import com.securevision.core.data.mapper.toEntity
import com.securevision.core.domain.repository.AlertRepository
import com.securevision.core.model.AlertRecord
import com.securevision.core.model.AlertType
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Room-backed alert storage.
 *
 * @property dao Alert table access.
 * @property dispatcherProvider Supplies the IO dispatcher for one-shot writes.
 */
@Singleton
class AlertRepositoryImpl @Inject constructor(
    private val dao: AlertDao,
    private val dispatcherProvider: DispatcherProvider,
) : AlertRepository {

    override suspend fun save(alert: AlertRecord) = withContext(dispatcherProvider.io) {
        dao.insert(alert.toEntity())
    }

    override fun getAll(): Flow<List<AlertRecord>> = dao.getAll().map { it.toDomain() }

    override fun getByType(type: AlertType): Flow<List<AlertRecord>> =
        dao.getByType(type).map { it.toDomain() }

    override fun getRecent(limit: Int): Flow<List<AlertRecord>> =
        dao.getRecent(limit).map { it.toDomain() }

    override fun getUnreadCount(): Flow<Int> = dao.getUnreadCount()

    override suspend fun markAllRead() = withContext(dispatcherProvider.io) {
        dao.markAllRead()
    }

    override suspend fun deleteOlderThan(timestamp: Long): Int =
        withContext(dispatcherProvider.io) { dao.deleteOlderThan(timestamp) }
}
