package com.securevision.ml.attributes

import android.graphics.Bitmap
import com.securevision.core.common.dispatcher.DispatcherProvider
import com.securevision.core.domain.engine.AttributeAnalysisEngine
import com.securevision.core.model.AttributeAvailability
import com.securevision.core.model.FaceAttributes
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.withContext

/** Wires [AttributeAnalyzer] behind the domain contract. */
@Singleton
class AttributeAnalysisEngineImpl @Inject constructor(
    private val analyzer: AttributeAnalyzer,
    private val dispatcherProvider: DispatcherProvider,
) : AttributeAnalysisEngine {

    override val availability: AttributeAvailability
        get() = analyzer.availability

    override suspend fun prepare(): AttributeAvailability =
        withContext(dispatcherProvider.default) { analyzer.load() }

    override suspend fun analyse(
        alignedFace: Bitmap,
        smilingProbability: Float?,
    ): FaceAttributes = withContext(dispatcherProvider.default) {
        analyzer.analyse(alignedFace, smilingProbability)
    }

    /** Releases every loaded interpreter. */
    fun shutdown() {
        analyzer.close()
    }
}
