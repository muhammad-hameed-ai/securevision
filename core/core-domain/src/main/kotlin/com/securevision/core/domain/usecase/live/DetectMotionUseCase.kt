package com.securevision.core.domain.usecase.live

import com.securevision.core.common.dispatcher.DispatcherProvider
import com.securevision.core.domain.engine.FaceFrame
import com.securevision.core.domain.engine.MotionDetectionEngine
import com.securevision.core.domain.usecase.UseCase
import com.securevision.core.model.MotionResult
import javax.inject.Inject

/**
 * Compares a frame against the previous one.
 *
 * The seam that keeps `feature-live` off `ml-motion`.
 */
class DetectMotionUseCase @Inject constructor(
    private val engine: MotionDetectionEngine,
    dispatcherProvider: DispatcherProvider,
) : UseCase<DetectMotionUseCase.Params, MotionResult>(dispatcherProvider.default) {

    /**
     * @property frame The frame to analyse.
     * @property intensityThreshold Changed-pixel fraction that counts as motion.
     */
    data class Params(
        val frame: FaceFrame,
        val intensityThreshold: Float,
    )

    override suspend fun execute(parameters: Params): MotionResult =
        engine.detect(parameters.frame, parameters.intensityThreshold)
}
