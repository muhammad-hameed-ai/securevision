package com.securevision.core.domain.engine

import com.securevision.core.model.WeaponDetection

/**
 * On-device weapon detection, as the presentation layer sees it.
 *
 * Declared here rather than in `:ml:ml-weapon` for the same reason as
 * [FaceRecognitionEngine]: `feature-live` must not depend on any `ml` module. A
 * compile probe in the verification step proves the boundary holds.
 */
interface WeaponDetectionEngine {

    /** Whether detection can run, and on what. */
    val status: EngineStatus

    /**
     * Loads the detector.
     *
     * @return The resulting status; a missing model is reported, never thrown.
     */
    suspend fun prepare(): EngineStatus

    /**
     * Finds weapons in a frame.
     *
     * @param frame The frame to analyse.
     * @param confidenceThreshold Minimum score, from settings. Set higher than the
     *   general object threshold because a weapon detection raises a critical
     *   alarm — a false positive costs far more here than a missed low-confidence
     *   frame.
     * @return One entry per weapon that survived filtering and suppression.
     *   Empty when the model is unavailable, never an error.
     */
    suspend fun detect(frame: FaceFrame, confidenceThreshold: Float): List<WeaponDetection>
}
