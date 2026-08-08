package com.securevision.core.domain.repository

import com.securevision.core.model.EnrolledProfile
import kotlinx.coroutines.flow.Flow

/**
 * Access to the **enrolled person profiles** used for live face recognition.
 *
 * Strictly on-device. Implementations must not synchronise this data anywhere:
 * the enrolment photo and the face embedding never leave the phone.
 */
interface EnrolledProfileRepository {

    /**
     * Inserts a profile, replacing any existing profile with the same
     * [EnrolledProfile.id].
     */
    suspend fun add(profile: EnrolledProfile)

    /** All enrolled profiles, newest first, re-emitted whenever the set changes. */
    fun getAll(): Flow<List<EnrolledProfile>>

    /**
     * Live count of enrolled profiles.
     *
     * Separate from [getAll] because the dashboard only needs the number:
     * counting by loading the list would read a 512-float embedding per person
     * just to call `size`.
     */
    fun countAll(): Flow<Int>

    /**
     * Looks up a single profile.
     *
     * @param id The profile identifier.
     * @return The profile, or `null` if no profile has that id.
     */
    suspend fun getById(id: String): EnrolledProfile?

    /**
     * Removes a profile and its enrolment photo.
     *
     * @param id The profile identifier.
     */
    suspend fun delete(id: String)

    /**
     * Profiles whose name matches [query], case-insensitively.
     *
     * @param query Search text; a blank query yields every profile.
     */
    fun search(query: String): Flow<List<EnrolledProfile>>
}
