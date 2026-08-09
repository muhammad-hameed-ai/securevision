package com.securevision.ml.motion

import com.securevision.core.common.dispatcher.DispatcherProvider
import com.securevision.core.domain.engine.FaceFrame
import com.securevision.core.domain.engine.MotionDetectionEngine
import com.securevision.core.model.MotionResult
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.withContext

/**
 * Wires the downscaler and the detector behind the domain contract.
 *
 * Needs no model and no load step, so unlike the face and weapon engines it is
 * ready the moment it is constructed — motion is the one detector that always
 * works.
 */
@Singleton
class MotionDetectionEngineImpl @Inject constructor(
    private val downscaler: LuminanceDownscaler,
    private val detector: MotionDetector,
    private val dispatcherProvider: DispatcherProvider,
) : MotionDetectionEngine {

    override suspend fun detect(
        frame: FaceFrame,
        intensityThreshold: Float,
    ): MotionResult = withContext(dispatcherProvider.default) {
        val grid = downscaler.downscale(frame.bitmap)

        detector.compare(
            current = grid,
            intensityThreshold = intensityThreshold,
            nowMillis = frame.timestampMillis,
        )
    }

    override fun reset() {
        detector.reset()
    }
}
