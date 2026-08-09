package com.securevision.core.data.storage

import android.graphics.Bitmap
import com.securevision.core.domain.engine.ProfilePhotoStore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Writes enrolment photos through [InternalStorageManager].
 *
 * A thin adapter so `core-domain` can persist a photo without knowing that
 * storage exists, and without `feature-live` gaining any filesystem access of its
 * own.
 */
@Singleton
class ProfilePhotoStoreImpl @Inject constructor(
    private val storageManager: InternalStorageManager,
) : ProfilePhotoStore {

    override suspend fun saveProfilePhoto(bitmap: Bitmap): String =
        storageManager.saveProfilePhoto(bitmap)
}
