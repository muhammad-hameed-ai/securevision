package com.securevision.ml.motion.di

import com.securevision.core.domain.engine.MotionDetectionEngine
import com.securevision.ml.motion.MotionDetectionEngineImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds motion detection to its domain contract.
 *
 * The only link between `feature-live` and this module. The feature declares no
 * dependency on it, so a Composable cannot reach the detector directly.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class MlMotionModule {

    @Binds
    @Singleton
    abstract fun bindMotionDetectionEngine(
        implementation: MotionDetectionEngineImpl,
    ): MotionDetectionEngine
}
