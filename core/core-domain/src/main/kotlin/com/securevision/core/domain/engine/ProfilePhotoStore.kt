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

    /**
     * Removes a photo that is no longer referenced.
     *
     * Used when re-enrolling replaces a profile's crop. Deleting the row is the
     * repository's job; this exists for the case where the row survives and only
     * the image is superseded.
     *
     * @param uri A URI previously returned by [saveProfilePhoto].
     * @return `true` if a file was removed. A `false` means the file was already
     *   gone, which is not worth failing an enrolment over.
     */
    suspend fun deleteProfilePhoto(uri: String): Boolean
}
