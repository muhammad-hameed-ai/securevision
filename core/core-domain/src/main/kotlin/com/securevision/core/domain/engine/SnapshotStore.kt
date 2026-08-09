package com.securevision.core.domain.engine

import android.graphics.Bitmap
import com.securevision.core.model.BoundingBox

/**
 * Persists the still image attached to an alert.
 *
 * Separate from [ProfilePhotoStore] despite both writing images: an enrolment
 * photo is reference data kept for the life of a profile, while a snapshot is
 * evidence subject to the retention policy. Sharing one contract would eventually
 * mean a retention sweep deleting enrolment photos.
 */
interface SnapshotStore {

    /**
     * Crops a frame to a detection and writes it.
     *
     * @param frame The full frame the detection came from.
     * @param region The detection's box in normalised coordinates. Padded slightly
     *   on save so the subject is not cut flush at the jawline, which makes a
     *   notification thumbnail much easier to recognise.
     * @return A `file://` URI, or `null` if the write failed. Returning `null`
     *   rather than throwing is deliberate: losing the image must not lose the
     *   alert record.
     */
    suspend fun saveSnapshot(frame: Bitmap, region: BoundingBox?): String?
}
