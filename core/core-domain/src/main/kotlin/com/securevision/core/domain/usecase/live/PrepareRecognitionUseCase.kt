package com.securevision.core.domain.usecase.live

import com.securevision.core.common.dispatcher.DispatcherProvider
import com.securevision.core.domain.engine.EngineStatus
import com.securevision.core.domain.engine.FaceRecognitionEngine
import com.securevision.core.domain.usecase.UseCase
import javax.inject.Inject

/**
 * Loads the recognition model and reports what happened.
 *
 * Run when the live screen opens rather than at graph construction, so the cost
 * of mapping a 45 MB asset is paid only when the camera is about to be used.
 */
class PrepareRecognitionUseCase @Inject constructor(
    private val engine: FaceRecognitionEngine,
    dispatcherProvider: DispatcherProvider,
) : UseCase<PrepareRecognitionUseCase.Params, EngineStatus>(dispatcherProvider.default) {

    /**
     * @property enrolledDimensions Embedding length of the stored profiles, so a
     *   model swap is reported instead of producing meaningless scores.
     */
    data class Params(val enrolledDimensions: Int?)

    override suspend fun execute(parameters: Params): EngineStatus =
        engine.prepare(parameters.enrolledDimensions)
}
