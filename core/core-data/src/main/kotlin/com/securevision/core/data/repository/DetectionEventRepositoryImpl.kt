package com.securevision.core.data.repository

import com.securevision.core.common.dispatcher.DispatcherProvider
import com.securevision.core.data.database.dao.DetectionEventDao
import com.securevision.core.data.mapper.toDomain
import com.securevision.core.data.mapper.toEntity
import com.securevision.core.domain.repository.DetectionEventRepository
import com.securevision.core.model.DetectionEvent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Room-backed detection audit trail.
 *
 * @property dao Detection event table access.
 * @property dispatcherProvider Supplies the IO dispatcher for one-shot writes.
 */
@Singleton
class DetectionEventRepositoryImpl @Inject constructor(
    private val dao: DetectionEventDao,
    private val dispatcherProvider: DispatcherProvider,
) : DetectionEventRepository {

    override suspend fun save(event: DetectionEvent) = withContext(dispatcherProvider.io) {
        dao.insert(event.toEntity())
    }

    override fun getAll(): Flow<List<DetectionEvent>> = dao.getAll().map { it.toDomain() }

    override fun countAll(): Flow<Int> = dao.countAll()
}
