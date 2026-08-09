package com.securevision.core.domain.usecase.live

import com.securevision.core.common.dispatcher.DispatcherProvider
import com.securevision.core.domain.repository.AlertRepository
import com.securevision.core.domain.repository.DetectionEventRepository
import com.securevision.core.domain.usecase.UseCase
import com.securevision.core.model.AlertRecord
import com.securevision.core.model.AlertType
import com.securevision.core.model.DetectionEvent
import com.securevision.core.model.FaceAttributes
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
     * @property snapshotUri Captured frame, or `null` if no image was saved.
     * @property attributes Soft attributes, when analysis was enabled and a
     *   classifier was available. Defaults to
     *   [com.securevision.core.model.FaceAttributes.NOT_ASSESSED], whose fields are
     *   all `null` — which is what reaches the database, so an alert never claims
     *   "no beard" when nothing looked.
     * @property timestamp When the sighting was confirmed, epoch milliseconds UTC.
     */
    data class Params(
        val confidence: Float,
        val cameraFacing: String,
        val snapshotUri: String? = null,
        val attributes: FaceAttributes = FaceAttributes.NOT_ASSESSED,
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
                // Passed straight through, nulls included. null means "not
                // assessed"; coercing it to false would be a claim about a person
                // no classifier ever examined.
                hasBeard = parameters.attributes.hasBeard,
                hasMask = parameters.attributes.hasMask,
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
