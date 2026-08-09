package com.securevision.ml.attributes

import android.graphics.Bitmap
import android.util.Log
import com.securevision.core.model.AttributeAvailability
import com.securevision.core.model.FaceAttributes
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton
import org.tensorflow.lite.Interpreter

/**
 * Infers soft attributes from an aligned face crop.
 *
 * **Every unavailable attribute reports `null`, never a default.** That is the
 * whole discipline of this class: an alert saying "no mask" when no mask
 * classifier was ever loaded is a false statement about a security event, and
 * false statements are worse than absent ones.
 *
 * Operates on the crop the recognition pipeline already aligned, so it adds one
 * inference per face rather than a second detection pass.
 */
@Singleton
class AttributeAnalyzer @Inject constructor(
    private val loader: AttributeModelLoader,
    private val coarseEmotion: CoarseEmotionClassifier,
) {

    /** Which classifiers are loaded. */
    var availability: AttributeAvailability = AttributeAvailability.NONE
        private set

    /**
     * Loads whichever models are present.
     *
     * @return What ended up available.
     */
    @Synchronized
    fun load(): AttributeAvailability {
        loader.loadAll()

        availability = AttributeAvailability(
            age = loader.isLoaded(AttributeModel.AGE),
            gender = loader.isLoaded(AttributeModel.GENDER),
            // Coarse emotion needs no model, so this attribute is always available
            // — the only one that is, until models are supplied.
            emotion = true,
            beard = loader.isLoaded(AttributeModel.BEARD),
            mask = loader.isLoaded(AttributeModel.MASK),
        )

        Log.i(TAG, "available attributes: ${availability.describe()}")

        return availability
    }

    /**
     * Analyses one aligned crop.
     *
     * @param alignedFace The 160×160 crop from the recognition pipeline.
     * @param smilingProbability Detector smile score, for the coarse emotion path.
     * @return Whatever could be assessed; everything else `null`.
     */
    fun analyse(alignedFace: Bitmap, smilingProbability: Float?): FaceAttributes =
        FaceAttributes(
            age = runBinary(AttributeModel.AGE, alignedFace)?.let(::toAge),
            gender = runBinary(AttributeModel.GENDER, alignedFace)?.let(::toGender),
            emotion = resolveEmotion(alignedFace, smilingProbability),
            hasBeard = runBinary(AttributeModel.BEARD, alignedFace)?.let { it >= DECISION_POINT },
            hasMask = runBinary(AttributeModel.MASK, alignedFace)?.let { it >= DECISION_POINT },
        )

    /** Releases every loaded interpreter. */
    fun close() {
        loader.close()
        availability = AttributeAvailability.NONE
    }

    /**
     * Runs a single-output classifier.
     *
     * @return The scalar output, or `null` when the model is absent or inference
     *   failed. Both cases are "not assessed" — a failed inference tells us
     *   nothing, so reporting a value would be inventing one.
     */
    private fun runBinary(model: AttributeModel, face: Bitmap): Float? {
        val interpreter = loader.interpreterFor(model) ?: return null

        return runCatching {
            val output = Array(1) { FloatArray(1) }
            interpreter.run(face.toInput(interpreter), output)
            output[0][0]
        }.onFailure { throwable ->
            Log.w(TAG, "${model.attributeName} inference failed", throwable)
        }.getOrNull()
    }

    private fun resolveEmotion(face: Bitmap, smilingProbability: Float?): String? {
        // A real model, when supplied, supersedes the smile heuristic entirely.
        loader.interpreterFor(AttributeModel.EMOTION)?.let { return runEmotionModel(face, it) }

        return coarseEmotion.classify(smilingProbability)
    }

    private fun runEmotionModel(face: Bitmap, interpreter: Interpreter): String? = runCatching {
        val classCount = interpreter.getOutputTensor(0).shape().last()
        val output = Array(1) { FloatArray(classCount) }
        interpreter.run(face.toInput(interpreter), output)

        val best = output[0].withIndex().maxByOrNull { it.value } ?: return null
        if (best.value < DECISION_POINT) return null

        EMOTION_LABELS.getOrNull(best.index)
    }.onFailure { throwable ->
        Log.w(TAG, "emotion inference failed", throwable)
    }.getOrNull()

    /** Maps a normalised regression output onto years. */
    private fun toAge(raw: Float): Int = (raw * MAX_AGE).toInt().coerceIn(MIN_AGE, MAX_AGE)

    private fun toGender(raw: Float): String = if (raw >= DECISION_POINT) MALE else FEMALE

    /**
     * Converts the crop to the classifier's input tensor.
     *
     * Reads the expected edge from the interpreter rather than assuming 160, since
     * attribute models are commonly trained at other sizes.
     */
    private fun Bitmap.toInput(interpreter: Interpreter): ByteBuffer {
        val shape = interpreter.getInputTensor(0).shape()
        val size = shape.getOrElse(1) { width }

        val scaled = if (width == size && height == size) {
            this
        } else {
            Bitmap.createScaledBitmap(this, size, size, true)
        }

        val buffer = ByteBuffer
            .allocateDirect(size * size * CHANNELS * Float.SIZE_BYTES)
            .order(ByteOrder.nativeOrder())

        val pixels = IntArray(size * size)
        scaled.getPixels(pixels, 0, size, 0, 0, size, size)
        if (scaled !== this) scaled.recycle()

        for (pixel in pixels) {
            buffer.putFloat(((pixel shr 16) and 0xFF) / MAX_CHANNEL)
            buffer.putFloat(((pixel shr 8) and 0xFF) / MAX_CHANNEL)
            buffer.putFloat((pixel and 0xFF) / MAX_CHANNEL)
        }

        buffer.rewind()
        return buffer
    }

    private companion object {
        const val TAG = "AttributeAnalyzer"
        const val CHANNELS = 3
        const val MAX_CHANNEL = 255f

        /** Sigmoid midpoint for a binary classifier. */
        const val DECISION_POINT = 0.5f

        const val MIN_AGE = 1
        const val MAX_AGE = 100

        const val MALE = "male"
        const val FEMALE = "female"

        /** Standard FER ordering, used when an emotion model is supplied. */
        val EMOTION_LABELS = listOf(
            "angry", "disgust", "fear", "happy", "sad", "surprise", "neutral",
        )
    }
}
