package com.securevision.core.domain.usecase.live

import com.securevision.core.common.dispatcher.DispatcherProvider
import com.securevision.core.domain.engine.FaceFrame
import com.securevision.core.domain.engine.FaceRecognitionEngine
import com.securevision.core.domain.usecase.UseCase
import com.securevision.core.model.AppSettings
import com.securevision.core.model.DetectionResult
import com.securevision.core.model.EnrolledProfile
import javax.inject.Inject

/**
 * Runs one camera frame through the recognition pipeline.
 *
 * The seam that keeps `feature-live` off `ml-face`: the ViewModel injects this,
 * not the detector, the aligner or TFLite.
 */
class RecogniseFacesUseCase @Inject constructor(
    private val engine: FaceRecognitionEngine,
    dispatcherProvider: DispatcherProvider,
) : UseCase<RecogniseFacesUseCase.Params, List<DetectionResult>>(dispatcherProvider.default) {

    /**
     * @property frame The frame to analyse.
     * @property profiles Enrolled profiles to match against.
     * @property settings Supplies the threshold, margin and vote count, so all
     *   three are tunable at runtime rather than baked into the pipeline.
     */
    data class Params(
        val frame: FaceFrame,
        val profiles: List<EnrolledProfile>,
        val settings: AppSettings,
    )

    override suspend fun execute(parameters: Params): List<DetectionResult> = engine.recognise(
        frame = parameters.frame,
        profiles = parameters.profiles,
        settings = parameters.settings,
    )
}
