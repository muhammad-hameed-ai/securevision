package com.securevision.core.domain.usecase.live

import com.securevision.core.common.dispatcher.DispatcherProvider
import com.securevision.core.domain.engine.AttributeAnalysisEngine
import com.securevision.core.domain.engine.EngineStatus
import com.securevision.core.domain.engine.FaceRecognitionEngine
import com.securevision.core.domain.engine.MotionDetectionEngine
import com.securevision.core.domain.engine.WeaponDetectionEngine
import com.securevision.core.domain.usecase.UseCase
import com.securevision.core.model.AttributeAvailability
import javax.inject.Inject

/**
 * Loads every detector when the live screen opens.
 *
 * One call rather than three, so the screen cannot accidentally start with only
 * some pipelines prepared. Each result is reported separately, because they fail
 * independently and their remedies differ — a missing face model needs a file, a
 * dimension mismatch needs re-enrolment, and a missing weapon model needs a
 * different file entirely.
 *
 * Loading happens here rather than at graph construction so the cost of mapping
 * tens of megabytes of models is paid when the camera is about to be used.
 */
class PrepareDetectorsUseCase @Inject constructor(
    private val faceEngine: FaceRecognitionEngine,
    private val weaponEngine: WeaponDetectionEngine,
    private val attributeEngine: AttributeAnalysisEngine,
    private val motionEngine: MotionDetectionEngine,
    dispatcherProvider: DispatcherProvider,
) : UseCase<PrepareDetectorsUseCase.Params, DetectorReadiness>(dispatcherProvider.default) {

    /**
     * @property enrolledDimensions Embedding length of the stored profiles, so a
     *   model swap is reported rather than producing meaningless scores.
     */
    data class Params(val enrolledDimensions: Int?)

    override suspend fun execute(parameters: Params): DetectorReadiness = DetectorReadiness(
        face = faceEngine.prepare(parameters.enrolledDimensions),
        weapon = weaponEngine.prepare(),
        attributes = attributeEngine.prepare(),
    )

    /**
     * Discards the motion baseline.
     *
     * Called on a camera flip: without it the lens change itself registers as one
     * enormous motion event, since every pixel differs from the previous frame.
     */
    suspend fun resetMotion() {
        motionEngine.reset()
    }

    /**
     * Discards multi-frame vote history.
     *
     * Also a camera-flip concern, and a subtler one. Vote history is keyed by
     * detector tracking id, and the new lens restarts those ids from zero. The
     * voter prunes ids that are absent, but when the new lens reuses id 1 — which
     * it usually does — that history is *kept* and a different person inherits
     * the previous person's votes, briefly putting the wrong name on the wrong
     * face.
     */
    fun resetTracking() {
        faceEngine.resetTracking()
    }
}

/**
 * What each detector managed to load.
 *
 * @property face Face recognition readiness.
 * @property weapon Weapon detection readiness.
 * @property attributes Which attribute classifiers are available; absent ones
 *   yield `null` attributes rather than defaults.
 */
data class DetectorReadiness(
    val face: EngineStatus,
    val weapon: EngineStatus,
    val attributes: AttributeAvailability,
)
