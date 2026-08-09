package com.securevision.ml.attributes

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import javax.inject.Inject
import javax.inject.Singleton
import org.tensorflow.lite.Interpreter

/**
 * Loads whichever attribute classifiers happen to be present.
 *
 * Each model is independent: supplying a beard classifier must not require also
 * supplying one for age. Anything absent stays absent, and the attribute it would
 * have produced reports `null` — "not assessed" — rather than a default.
 *
 * The available set is logged at load so a missing model is a visible fact rather
 * than a silently empty field in an alert.
 */
@Singleton
class AttributeModelLoader @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    private val interpreters = mutableMapOf<AttributeModel, Interpreter>()

    /** Attempts to load every known classifier. Safe to call repeatedly. */
    @Synchronized
    fun loadAll() {
        AttributeModel.entries.forEach { model ->
            if (interpreters.containsKey(model)) return@forEach

            val buffer = readAsset(model.assetName)
            if (buffer == null) {
                Log.i(TAG, "${model.assetName} not present — ${model.attributeName} not assessed")
                return@forEach
            }

            runCatching { Interpreter(buffer) }
                .onSuccess { interpreter ->
                    interpreters[model] = interpreter
                    Log.i(
                        TAG,
                        "${model.assetName} loaded: input " +
                            "${interpreter.getInputTensor(0).shape().joinToString()}, output " +
                            "${interpreter.getOutputTensor(0).shape().joinToString()}",
                    )
                }
                .onFailure { throwable ->
                    Log.e(TAG, "${model.assetName} failed to load", throwable)
                }
        }
    }

    /**
     * @param model The classifier to look up.
     * @return Its interpreter, or `null` when it is not loaded.
     */
    fun interpreterFor(model: AttributeModel): Interpreter? = interpreters[model]

    /** @return `true` when [model] can run. */
    fun isLoaded(model: AttributeModel): Boolean = interpreters.containsKey(model)

    /** Releases every loaded interpreter. */
    @Synchronized
    fun close() {
        interpreters.values.forEach { interpreter -> runCatching { interpreter.close() } }
        interpreters.clear()
    }

    private fun readAsset(name: String): MappedByteBuffer? = runCatching {
        context.assets.openFd(name).use { descriptor ->
            descriptor.createInputStream().use { stream ->
                stream.channel.map(
                    FileChannel.MapMode.READ_ONLY,
                    descriptor.startOffset,
                    descriptor.declaredLength,
                )
            }
        }
    }.getOrNull()

    private companion object {
        const val TAG = "AttributeAnalyzer"
    }
}

/**
 * The attribute classifiers this module knows how to load.
 *
 * @property assetName File expected in `ml-attributes/src/main/assets`.
 * @property attributeName Human-readable name, for the load log.
 */
enum class AttributeModel(val assetName: String, val attributeName: String) {
    /** Beard presence. */
    BEARD("face_beard.tflite", "beard"),

    /** Face covering presence. */
    MASK("face_mask.tflite", "mask"),

    /** Age estimation. */
    AGE("face_age.tflite", "age"),

    /** Gender estimation. */
    GENDER("face_gender.tflite", "gender"),

    /** Emotion classification, superseding the coarse smile-based signal. */
    EMOTION("face_emotion.tflite", "emotion"),
}
