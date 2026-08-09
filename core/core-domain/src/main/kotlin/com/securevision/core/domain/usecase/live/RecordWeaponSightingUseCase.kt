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
 * Records a confirmed weapon sighting.
 *
 * Always [Severity.CRITICAL]: a weapon is the one detection this product treats
 * as unconditionally urgent, and downgrading it on low confidence would be the
 * wrong trade — the confidence threshold already sits higher than every other
 * detector's precisely so that anything reaching here is worth interrupting for.
 */
class RecordWeaponSightingUseCase @Inject constructor(
    private val alertRepository: AlertRepository,
    private val detectionEventRepository: DetectionEventRepository,
    dispatcherProvider: DispatcherProvider,
) : UseCase<RecordWeaponSightingUseCase.Params, Unit>(dispatcherProvider.io) {

    /**
     * @property weaponType Detector class label, e.g. `"knife"`.
     * @property confidence Detector score in `0f..1f`.
     * @property cameraFacing Which camera saw it, `"front"` or `"back"`.
     * @property snapshotUri Captured frame, or `null` if the write failed.
     * @property timestamp When it was confirmed, epoch milliseconds UTC.
     */
    data class Params(
        val weaponType: String,
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
                type = AlertType.WEAPON,
                severity = Severity.CRITICAL,
                confidence = parameters.confidence,
                cameraFacing = parameters.cameraFacing,
                snapshotUri = parameters.snapshotUri,
                // No face was analysed for a weapon detection, so these stay null.
                // false would be a claim about a person nothing looked at.
                hasBeard = null,
                hasMask = null,
                timestamp = parameters.timestamp,
                isRead = false,
            ),
        )

        detectionEventRepository.save(
            DetectionEvent(
                id = id,
                type = AlertType.WEAPON,
                label = parameters.weaponType,
                confidence = parameters.confidence,
                cameraFacing = parameters.cameraFacing,
                timestamp = parameters.timestamp,
            ),
        )
    }
}
