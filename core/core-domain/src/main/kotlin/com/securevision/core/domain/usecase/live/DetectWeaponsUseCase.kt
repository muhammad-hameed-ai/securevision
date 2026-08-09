package com.securevision.core.domain.usecase.live

import com.securevision.core.common.dispatcher.DispatcherProvider
import com.securevision.core.domain.engine.FaceFrame
import com.securevision.core.domain.engine.WeaponDetectionEngine
import com.securevision.core.domain.usecase.UseCase
import com.securevision.core.model.WeaponDetection
import javax.inject.Inject

/**
 * Finds weapons in a frame.
 *
 * The seam that keeps `feature-live` off `ml-weapon`.
 */
class DetectWeaponsUseCase @Inject constructor(
    private val engine: WeaponDetectionEngine,
    dispatcherProvider: DispatcherProvider,
) : UseCase<DetectWeaponsUseCase.Params, List<WeaponDetection>>(dispatcherProvider.default) {

    /**
     * @property frame The frame to analyse.
     * @property confidenceThreshold Minimum detector score, from settings.
     */
    data class Params(
        val frame: FaceFrame,
        val confidenceThreshold: Float,
    )

    override suspend fun execute(parameters: Params): List<WeaponDetection> =
        engine.detect(parameters.frame, parameters.confidenceThreshold)
}
