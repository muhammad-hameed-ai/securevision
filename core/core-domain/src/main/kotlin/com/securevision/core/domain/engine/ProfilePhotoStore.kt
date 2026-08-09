package com.securevision.core.domain.engine

import android.graphics.Bitmap

/**
 * Persists enrolment photos to on-device storage.
 *
 * A narrow contract rather than exposing the whole storage manager, because the
 * only thing the enrolment path needs is "turn this bitmap into a URI I can store
 * on a profile". Implemented in `core-data`, which is the only module allowed to
 * touch the filesystem.
 */
interface ProfilePhotoStore {

    /**
     * Writes an enrolment photo.
     *
     * @param bitmap The aligned face crop — the same image the embedder saw, so
     *   the stored photo shows what was actually enrolled rather than a wider shot
     *   that merely contains it.
     * @return A `file://` URI inside the app's private storage.
     */
    suspend fun saveProfilePhoto(bitmap: Bitmap): String
}
