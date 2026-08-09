package com.securevision.core.domain.usecase.live

import com.securevision.core.common.dispatcher.DispatcherProvider
import com.securevision.core.domain.repository.AlertRepository
import com.securevision.core.domain.repository.DetectionEventRepository
import com.securevision.core.domain.usecase.UseCase
import com.securevision.core.model.AlertRecord
import com.securevision.core.model.AlertType
import com.securevision.core.model.DetectionEvent
import com.securevision.core.model.Severity
import java.util.UUID
import javax.inject.Inject

/**
 * Records a motion event in an otherwise static scene.
 *
 * [Severity.LOW] on purpose. Movement is the weakest signal this app produces —
 * a curtain, a pet, a change in light all trigger it — and rating it any higher
 * would train the operator to dismiss the severity badge entirely, which would
 * then also be dismissed on the weapon alert that matters.
 *
 * Called only for a debounced event, never per frame.
 */
class RecordMotionSightingUseCase @Inject constructor(
    private val alertRepository: AlertRepository,
    private val detectionEventRepository: DetectionEventRepository,
    dispatcherProvider: DispatcherProvider,
) : UseCase<RecordMotionSightingUseCase.Params, Unit>(dispatcherProvider.io) {

    /**
     * @property intensity Fraction of the frame that changed, in `0f..1f`.
     * @property cameraFacing Which camera saw it.
     * @property snapshotUri Captured frame, or `null` if the write failed.
     * @property timestamp When it was confirmed, epoch milliseconds UTC.
     */
    data class Params(
        val intensity: Float,
        val cameraFacing: String,
        val snapshotUri: String? = null,
        val timestamp: Long = System.currentTimeMillis(),
    )

    override suspend fun execute(parameters: Params) {
        val id = UUID.randomUUID().toString()

        alertRepository.save(
            AlertRecord(
                id = id,
                type = AlertType.MOTION,
                severity = Severity.LOW,
                confidence = parameters.intensity,
                cameraFacing = parameters.cameraFacing,
                snapshotUri = parameters.snapshotUri,
                // Motion says nothing about a face; nothing looked.
                hasBeard = null,
                hasMask = null,
                timestamp = parameters.timestamp,
                isRead = false,
            ),
        )

        detectionEventRepository.save(
            DetectionEvent(
                id = id,
                type = AlertType.MOTION,
                label = MOTION_LABEL,
                confidence = parameters.intensity,
                cameraFacing = parameters.cameraFacing,
                timestamp = parameters.timestamp,
            ),
        )
    }

    private companion object {
        /** Not user-facing: History maps the type onto a localised string. */
        const val MOTION_LABEL = "motion"
    }
}
