package com.securevision.ml.attributes.di

import com.securevision.core.domain.engine.AttributeAnalysisEngine
import com.securevision.ml.attributes.AttributeAnalysisEngineImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds attribute analysis to its domain contract. */
@Module
@InstallIn(SingletonComponent::class)
abstract class MlAttributesModule {

    @Binds
    @Singleton
    abstract fun bindAttributeAnalysisEngine(
        implementation: AttributeAnalysisEngineImpl,
    ): AttributeAnalysisEngine
}
