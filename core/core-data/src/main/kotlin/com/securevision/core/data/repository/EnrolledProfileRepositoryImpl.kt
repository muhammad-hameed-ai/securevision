package com.securevision.core.data.repository

import com.securevision.core.common.dispatcher.DispatcherProvider
import com.securevision.core.data.database.dao.EnrolledProfileDao
import com.securevision.core.data.mapper.toDomain
import com.securevision.core.data.mapper.toEntity
import com.securevision.core.data.storage.InternalStorageManager
import com.securevision.core.domain.repository.EnrolledProfileRepository
import com.securevision.core.model.EnrolledProfile
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Room-backed enrolled person profiles. On-device only, by design.
 *
 * @property dao Profile table access.
 * @property storageManager Owns the enrolment photo files.
 * @property dispatcherProvider Supplies the IO dispatcher for one-shot writes.
 */
@Singleton
class EnrolledProfileRepositoryImpl @Inject constructor(
    private val dao: EnrolledProfileDao,
    private val storageManager: InternalStorageManager,
    private val dispatcherProvider: DispatcherProvider,
) : EnrolledProfileRepository {

    override suspend fun add(profile: EnrolledProfile) = withContext(dispatcherProvider.io) {
        dao.insert(profile.toEntity())
    }

    override fun getAll(): Flow<List<EnrolledProfile>> = dao.getAll().map { it.toDomain() }

    /**
     * Derived from [getAll] rather than a separate `COUNT(*)` query.
     *
     * One source of truth for "who is enrolled". The dedicated count query was
     * more efficient — it avoided reading a 512-float embedding per person just
     * to produce a number — but it meant the Profiles list and the Dashboard
     * figure were two independent observers of the same table, able to disagree.
     * A dashboard reading zero beside a list showing someone is worse than the
     * cost of the extra bytes, and enrolment counts here are in the tens.
     */
    override fun countAll(): Flow<Int> = getAll().map { profiles -> profiles.size }

    override suspend fun getById(id: String): EnrolledProfile? =
        withContext(dispatcherProvider.io) { dao.getById(id)?.toDomain() }

    /**
     * Removes the row and its enrolment photo.
     *
     * The photo is deleted first so that a failure between the two steps leaves an
     * orphaned file rather than a profile whose photo has vanished — the former is
     * reclaimable, the latter renders a broken row in the UI.
     */
    override suspend fun delete(id: String) = withContext(dispatcherProvider.io) {
        dao.getById(id)?.let { existing -> storageManager.deleteFile(existing.photoUri) }
        dao.delete(id)
    }

    override fun search(query: String): Flow<List<EnrolledProfile>> {
        val trimmed = query.trim()

        return if (trimmed.isEmpty()) {
            getAll()
        } else {
            dao.search(trimmed).map { it.toDomain() }
        }
    }
}
