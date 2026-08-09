package com.securevision.ml.face.di

import com.securevision.core.domain.engine.FaceRecognitionEngine
import com.securevision.ml.face.FaceRecognitionEngineImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds the on-device face pipeline to its domain contract.
 *
 * This binding is the only thing connecting `feature-live` to `ml-face`. The
 * feature module declares no dependency on this one, so a Composable cannot
 * reach the detector, the aligner or TFLite directly — a compile probe in the
 * verification step proves it.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class MlFaceModule {

    @Binds
    @Singleton
    abstract fun bindFaceRecognitionEngine(
        implementation: FaceRecognitionEngineImpl,
    ): FaceRecognitionEngine
}
