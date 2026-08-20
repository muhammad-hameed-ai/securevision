package com.securevision.core.domain.usecase.profile

import com.securevision.core.common.dispatcher.DispatcherProvider
import com.securevision.core.domain.engine.EnrolmentCapture
import com.securevision.core.domain.engine.FaceFrame
import com.securevision.core.domain.engine.FaceRecognitionEngine
import com.securevision.core.domain.usecase.UseCase
import javax.inject.Inject

/**
 * Runs detect → quality gate → **align** → embed on a captured frame.
 *
 * **This is the only place in the app that calls
 * [FaceRecognitionEngine.embedForEnrolment].** Everything that enrols a face goes
 * through here, so an enrolment embedding is produced by exactly the same
 * alignment and inference that recognition uses. A second call site anywhere else
 * would let the two drift, and the symptom of drift is uniformly low similarity
 * for everybody — the precise failure this pipeline exists to prevent.
 *
 * Deliberately does not persist anything. Separating capture from saving is what
 * lets the enrolment screen show the operator the aligned crop *before* it is
 * committed, so a badly framed enrolment is caught by looking at it rather than
 * discovered weeks later in the match scores. [SaveEnrolmentUseCase] writes the
 * result.
 */
class CaptureEnrolmentUseCase @Inject constructor(
    private val engine: FaceRecognitionEngine,
    dispatcherProvider: DispatcherProvider,
) : UseCase<CaptureEnrolmentUseCase.Params, EnrolmentCapture>(dispatcherProvider.default) {

    /**
     * @property frame The frame to enrol from.
     */
    data class Params(val frame: FaceFrame)

    override suspend fun execute(parameters: Params): EnrolmentCapture =
        engine.embedForEnrolment(parameters.frame)
}
