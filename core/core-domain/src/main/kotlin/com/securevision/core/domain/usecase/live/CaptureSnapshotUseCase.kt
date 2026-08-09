package com.securevision.core.domain.usecase.live

import com.securevision.core.domain.engine.FaceFrame
import com.securevision.core.domain.engine.SnapshotStore
import com.securevision.core.model.BoundingBox
import javax.inject.Inject

/**
 * Writes the still image attached to an alert.
 *
 * Deliberately **not** a [com.securevision.core.domain.usecase.UseCase]. The base
 * class converts failures into `Result.Error`, which would invite a caller to
 * treat a failed capture as a failed alert. A snapshot is supplementary evidence:
 * losing it must never lose the record, so this returns a nullable URI and the
 * alert is written either way.
 */
class CaptureSnapshotUseCase @Inject constructor(
    private val snapshotStore: SnapshotStore,
) {

    /**
     * Crops a frame to a detection and saves it.
     *
     * @param frame The frame the detection came from.
     * @param region The detection's box, or `null` to save the whole frame — which
     *   is what a motion event wants, since there is no subject to crop to.
     * @return A `file://` URI, or `null` when the write failed.
     */
    suspend operator fun invoke(frame: FaceFrame, region: BoundingBox?): String? =
        snapshotStore.saveSnapshot(frame = frame.bitmap, region = region)
}
