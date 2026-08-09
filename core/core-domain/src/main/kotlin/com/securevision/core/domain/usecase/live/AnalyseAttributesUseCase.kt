package com.securevision.core.domain.usecase.live

import com.securevision.core.common.dispatcher.DispatcherProvider
import com.securevision.core.domain.engine.AttributeAnalysisEngine
import com.securevision.core.domain.engine.RecognisedFace
import com.securevision.core.domain.usecase.UseCase
import com.securevision.core.model.FaceAttributes
import javax.inject.Inject

/**
 * Infers soft attributes for a recognised face.
 *
 * Operates on the aligned crop the recognition pipeline already produced. A face
 * with no crop — because it never reached alignment, or crops were not requested
 * — yields [FaceAttributes.NOT_ASSESSED], every field `null`. That is the correct
 * answer rather than a failure: nothing looked, so nothing is known.
 */
class AnalyseAttributesUseCase @Inject constructor(
    private val engine: AttributeAnalysisEngine,
    dispatcherProvider: DispatcherProvider,
) : UseCase<AnalyseAttributesUseCase.Params, FaceAttributes>(dispatcherProvider.default) {

    /**
     * @property face The recognised face, carrying its aligned crop.
     */
    data class Params(val face: RecognisedFace)

    override suspend fun execute(parameters: Params): FaceAttributes {
        val crop = parameters.face.alignedCrop ?: return FaceAttributes.NOT_ASSESSED

        return engine.analyse(
            alignedFace = crop,
            smilingProbability = parameters.face.smilingProbability,
        )
    }
}
