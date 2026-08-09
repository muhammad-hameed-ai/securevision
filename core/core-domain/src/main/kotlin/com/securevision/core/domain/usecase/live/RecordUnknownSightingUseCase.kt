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
 * Records a confirmed sighting of an unrecognised person.
 *
 * Called only once voting has committed to UNKNOWN, never on a single frame's
 * verdict — a stranger glimpsed for one blurred frame is not a security event,
 * and treating it as one would fill the alert list with noise before Phase 5 ever
 * attaches a notification to it.
 *
 * Writes both an alert, which the operator sees and can mark read, and a
 * detection event, which is the raw audit trail.
 */
class RecordUnknownSightingUseCase @Inject constructor(
    private val alertRepository: AlertRepository,
    private val detectionEventRepository: DetectionEventRepository,
    dispatcherProvider: DispatcherProvider,
) : UseCase<RecordUnknownSightingUseCase.Params, Unit>(dispatcherProvider.io) {

    /**
     * @property confidence Best similarity seen, even though it was rejected.
     * @property cameraFacing Which camera saw them, `"front"` or `"back"`.
     * @property snapshotUri Captured frame, when one was saved. Phase 5 fills this
     *   in; Phase 4 records the sighting without an image.
     * @property timestamp When the sighting was confirmed, epoch milliseconds UTC.
     */
    data class Params(
        val confidence: Float,
        val cameraFacing: String,
        val snapshotUri: String? = null,
        val timestamp: Long = System.currentTimeMillis(),
    )

    override suspend fun execute(parameters: Params) {
        val id = UUID.randomUUID().toString()

        alertRepository.save(
            AlertRecord(
                id = id,
                type = AlertType.UNKNOWN_PERSON,
                severity = Severity.HIGH,
                confidence = parameters.confidence,
                cameraFacing = parameters.cameraFacing,
                snapshotUri = parameters.snapshotUri,
                // Beard and mask are Phase 5. Left null rather than false, because
                // null means "not assessed" and false would be a claim.
                hasBeard = null,
                hasMask = null,
                timestamp = parameters.timestamp,
                isRead = false,
            ),
        )

        detectionEventRepository.save(
            DetectionEvent(
                id = id,
                type = AlertType.UNKNOWN_PERSON,
                label = UNKNOWN_LABEL,
                confidence = parameters.confidence,
                cameraFacing = parameters.cameraFacing,
                timestamp = parameters.timestamp,
            ),
        )
    }

    private companion object {
        /** Not user-facing: the History screen maps the type onto a localised string. */
        const val UNKNOWN_LABEL = "unknown_person"
    }
}
