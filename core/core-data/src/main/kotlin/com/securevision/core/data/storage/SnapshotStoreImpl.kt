package com.securevision.core.data.storage

import android.graphics.Bitmap
import android.util.Log
import com.securevision.core.common.dispatcher.DispatcherProvider
import com.securevision.core.domain.engine.SnapshotStore
import com.securevision.core.model.BoundingBox
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.withContext

/**
 * Writes alert snapshots through [InternalStorageManager].
 *
 * Crops to the detection with a margin, so the saved image is recognisable as a
 * person rather than a rectangle of cheek. A motion event passes `null` for the
 * region and gets the whole frame, since there is no subject to crop to.
 */
@Singleton
class SnapshotStoreImpl @Inject constructor(
    private val storageManager: InternalStorageManager,
    private val dispatcherProvider: DispatcherProvider,
) : SnapshotStore {

    /**
     * Crops and saves.
     *
     * Returns `null` on any failure rather than throwing: a snapshot is
     * supplementary evidence, and losing the image must never lose the alert.
     */
    override suspend fun saveSnapshot(
        frame: Bitmap,
        region: BoundingBox?,
    ): String? = withContext(dispatcherProvider.default) {
        runCatching {
            val cropped = region?.let { frame.cropTo(it) } ?: frame

            val uri = storageManager.saveSnapshot(cropped)

            if (cropped !== frame) cropped.recycle()

            uri
        }.onFailure { throwable ->
            Log.w(TAG, "snapshot capture failed; the alert is still recorded", throwable)
        }.getOrNull()
    }

    /**
     * Crops to a normalised region, padded and clamped to the frame.
     *
     * Every bound is clamped because a detection box can extend past the frame
     * edge when a subject is partly out of shot, and `createBitmap` throws rather
     * than clipping.
     */
    private fun Bitmap.cropTo(region: BoundingBox): Bitmap {
        val padX = region.width * PADDING_FRACTION
        val padY = region.height * PADDING_FRACTION

        val left = ((region.left - padX) * width).toInt().coerceIn(0, width - 1)
        val top = ((region.top - padY) * height).toInt().coerceIn(0, height - 1)
        val right = ((region.right + padX) * width).toInt().coerceIn(left + 1, width)
        val bottom = ((region.bottom + padY) * height).toInt().coerceIn(top + 1, height)

        return Bitmap.createBitmap(this, left, top, right - left, bottom - top)
    }

    private companion object {
        const val TAG = "SnapshotStore"

        /**
         * Margin added around the detection, as a fraction of its size.
         *
         * A box cropped flush to the jawline is hard to recognise in a
         * notification thumbnail; a little context makes it obvious.
         */
        const val PADDING_FRACTION = 0.25f
    }
}
