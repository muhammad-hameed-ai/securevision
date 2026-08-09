package com.securevision.ml.weapon

import com.securevision.core.common.dispatcher.DispatcherProvider
import com.securevision.core.domain.engine.EngineStatus
import com.securevision.core.domain.engine.FaceFrame
import com.securevision.core.domain.engine.WeaponDetectionEngine
import com.securevision.core.model.WeaponDetection
import com.securevision.ml.weapon.detect.WeaponDetector
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.withContext

/** Wires [WeaponDetector] behind the domain contract. */
@Singleton
class WeaponDetectionEngineImpl @Inject constructor(
    private val detector: WeaponDetector,
    private val dispatcherProvider: DispatcherProvider,
) : WeaponDetectionEngine {

    override val status: EngineStatus
        get() = detector.status

    override suspend fun prepare(): EngineStatus = withContext(dispatcherProvider.default) {
        detector.load()
        detector.status
    }

    override suspend fun detect(
        frame: FaceFrame,
        confidenceThreshold: Float,
    ): List<WeaponDetection> = withContext(dispatcherProvider.default) {
        // An absent model yields no detections rather than an error: weapons being
        // unavailable must not stop faces and motion from working.
        if (!detector.isReady) return@withContext emptyList()

        detector.detect(frame.bitmap, confidenceThreshold)
    }

    /** Releases the interpreter. */
    fun shutdown() {
        detector.close()
    }
}
