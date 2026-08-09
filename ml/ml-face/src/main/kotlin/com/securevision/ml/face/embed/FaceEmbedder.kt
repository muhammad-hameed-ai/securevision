package com.securevision.ml.face.embed

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.securevision.core.domain.engine.EngineStatus
import com.securevision.core.domain.engine.InferenceDelegate
import com.securevision.ml.face.align.AlignmentTemplate
import dagger.hilt.android.qualifiers.ApplicationContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt
import org.tensorflow.lite.Delegate
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.nnapi.NnApiDelegate

/**
 * Turns an aligned face crop into an embedding vector.
 *
 * Two properties of this class matter more than the inference itself:
 *
 * 1. **The output dimension is read from the model, never assumed.** A model
 *    emitting 128 or 192 dimensions where 512 was expected would otherwise be
 *    compared against stored 512-vectors and produce plausible-looking nonsense.
 * 2. **A missing or unloadable model is a reported state, not a crash.**
 *    Detection, alignment and overlay all keep working; only recognition stops,
 *    and the UI says so.
 *
 * Everything it discovers at load — the asset list, tensor shapes, active
 * delegate — is logged under [TAG], because a silent CPU fallback is the kind of
 * problem that only shows up as "why is this slow" three phases later.
 */
@Singleton
class FaceEmbedder @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    private var interpreter: Interpreter? = null
    private var delegate: Delegate? = null

    /** Current readiness. Reported upward so the UI can explain itself. */
    var status: EngineStatus = EngineStatus.Initialising
        private set

    /** Output dimension of the loaded model, or `null` when nothing is loaded. */
    var embeddingDimensions: Int? = null
        private set

    /** Whether [embed] can produce a vector. */
    val isReady: Boolean get() = interpreter != null && embeddingDimensions != null

    /**
     * Loads the model, selecting the fastest delegate the device supports.
     *
     * Safe to call repeatedly; subsequent calls are no-ops once loaded.
     */
    @Synchronized
    fun load() {
        if (interpreter != null) return

        logAvailableAssets()

        val modelBuffer = readModelAsset()
        if (modelBuffer == null) {
            Log.e(TAG, "$MODEL_ASSET not found in assets — recognition disabled")
            status = EngineStatus.RecognitionUnavailable(
                EngineStatus.RecognitionUnavailable.Reason.MODEL_NOT_INSTALLED,
            )
            return
        }

        val (createdInterpreter, activeDelegate, createdDelegate) = createInterpreter(modelBuffer)

        if (createdInterpreter == null) {
            status = EngineStatus.RecognitionUnavailable(
                EngineStatus.RecognitionUnavailable.Reason.MODEL_LOAD_FAILED,
            )
            return
        }

        val inputShape = createdInterpreter.getInputTensor(0).shape()
        val outputShape = createdInterpreter.getOutputTensor(0).shape()
        val dimensions = outputShape.last()

        Log.i(TAG, "model loaded: $MODEL_ASSET")
        Log.i(TAG, "  input  shape = ${inputShape.joinToString()} " +
            "dtype = ${createdInterpreter.getInputTensor(0).dataType()}")
        Log.i(TAG, "  output shape = ${outputShape.joinToString()} " +
            "dtype = ${createdInterpreter.getOutputTensor(0).dataType()}")
        Log.i(TAG, "  EMBEDDING DIMENSIONS = $dimensions")
        Log.i(TAG, "  delegate = $activeDelegate")

        if (inputShape.size != EXPECTED_INPUT_RANK ||
            inputShape[1] != AlignmentTemplate.OUTPUT_SIZE ||
            inputShape[2] != AlignmentTemplate.OUTPUT_SIZE
        ) {
            Log.e(
                TAG,
                "model expects input ${inputShape.joinToString()} but this pipeline aligns to " +
                    "${AlignmentTemplate.OUTPUT_SIZE}x${AlignmentTemplate.OUTPUT_SIZE} — refusing to load",
            )
            createdInterpreter.close()
            createdDelegate?.close()
            status = EngineStatus.RecognitionUnavailable(
                EngineStatus.RecognitionUnavailable.Reason.MODEL_LOAD_FAILED,
            )
            return
        }

        interpreter = createdInterpreter
        delegate = createdDelegate
        embeddingDimensions = dimensions
        status = EngineStatus.Ready(embeddingDimensions = dimensions, delegate = activeDelegate)
    }

    /**
     * Embeds an aligned face crop.
     *
     * @param alignedFace A crop produced by
     *   [com.securevision.ml.face.align.FaceAligner]. Passing an unaligned bitmap
     *   here compiles and runs, and is exactly the mistake that made the previous
     *   app score 0.23 for everyone.
     * @return An L2-normalised embedding, or `null` when the model is unavailable
     *   or inference fails.
     */
    fun embed(alignedFace: Bitmap): FloatArray? {
        val activeInterpreter = interpreter ?: return null
        val dimensions = embeddingDimensions ?: return null

        return runCatching {
            val input = alignedFace.toNormalisedInput()
            val output = Array(1) { FloatArray(dimensions) }

            activeInterpreter.run(input, output)

            output[0].l2Normalised()
        }.onFailure { throwable ->
            Log.e(TAG, "inference failed", throwable)
        }.getOrNull()
    }

    /** Releases the interpreter and any delegate. */
    @Synchronized
    fun close() {
        runCatching { interpreter?.close() }
        runCatching { delegate?.close() }
        interpreter = null
        delegate = null
        embeddingDimensions = null
        status = EngineStatus.Initialising
    }

    /**
     * Tries GPU, then NNAPI, then CPU.
     *
     * Each fallback is logged with its cause. A device that quietly runs on CPU
     * because the GPU delegate threw is indistinguishable from one that has no
     * GPU, and the two need different responses.
     */
    private fun createInterpreter(
        model: MappedByteBuffer,
    ): Triple<Interpreter?, InferenceDelegate, Delegate?> {
        val compatibility = runCatching { CompatibilityList() }.getOrNull()

        if (compatibility?.isDelegateSupportedOnThisDevice == true) {
            val attempt = runCatching {
                val gpuDelegate = GpuDelegate(compatibility.bestOptionsForThisDevice)
                val options = Interpreter.Options().addDelegate(gpuDelegate)
                Interpreter(model, options) to gpuDelegate
            }

            attempt.getOrNull()?.let { (created, gpuDelegate) ->
                return Triple(created, InferenceDelegate.GPU, gpuDelegate)
            }
            Log.w(TAG, "GPU delegate unavailable, trying NNAPI", attempt.exceptionOrNull())
        } else {
            Log.i(TAG, "GPU delegate not supported on this device, trying NNAPI")
        }

        val nnApiAttempt = runCatching {
            val nnApiDelegate = NnApiDelegate()
            val options = Interpreter.Options().addDelegate(nnApiDelegate)
            Interpreter(model, options) to nnApiDelegate
        }

        nnApiAttempt.getOrNull()?.let { (created, nnApiDelegate) ->
            return Triple(created, InferenceDelegate.NNAPI, nnApiDelegate)
        }
        Log.w(TAG, "NNAPI delegate unavailable, falling back to CPU", nnApiAttempt.exceptionOrNull())

        val cpuAttempt = runCatching {
            Interpreter(model, Interpreter.Options().setNumThreads(CPU_THREADS))
        }

        cpuAttempt.exceptionOrNull()?.let { throwable ->
            Log.e(TAG, "CPU interpreter failed — model is unusable", throwable)
        }

        return Triple(cpuAttempt.getOrNull(), InferenceDelegate.CPU, null)
    }

    /**
     * Memory-maps the model asset.
     *
     * Mapping rather than reading into the heap is why the `.tflite` must not be
     * compressed in the APK — see the `noCompress` setting in the application
     * convention plugin. A compressed asset has no mappable region and fails here.
     */
    private fun readModelAsset(): MappedByteBuffer? = runCatching {
        context.assets.openFd(MODEL_ASSET).use { descriptor ->
            descriptor.createInputStream().use { stream ->
                stream.channel.map(
                    FileChannel.MapMode.READ_ONLY,
                    descriptor.startOffset,
                    descriptor.declaredLength,
                )
            }
        }
    }.onFailure { throwable ->
        Log.w(TAG, "could not map $MODEL_ASSET", throwable)
    }.getOrNull()

    private fun logAvailableAssets() {
        val assets = runCatching { context.assets.list("")?.toList().orEmpty() }
            .getOrDefault(emptyList())
        Log.i(TAG, "assets present: ${assets.joinToString().ifEmpty { "<none>" }}")
    }

    /**
     * Converts the crop to the model's input tensor.
     *
     * `(pixel − 127.5) / 128` maps 0..255 onto roughly −1..1, which is the
     * normalisation FaceNet was trained with. Getting this wrong does not throw;
     * it just shifts every embedding into a region of the space the model never
     * saw during training.
     */
    private fun Bitmap.toNormalisedInput(): ByteBuffer {
        val size = AlignmentTemplate.OUTPUT_SIZE
        val buffer = ByteBuffer
            .allocateDirect(size * size * CHANNELS * Float.SIZE_BYTES)
            .order(ByteOrder.nativeOrder())

        val pixels = IntArray(size * size)
        getPixels(pixels, 0, size, 0, 0, size, size)

        for (pixel in pixels) {
            buffer.putFloat(((pixel shr 16 and 0xFF) - MEAN) / STD)
            buffer.putFloat(((pixel shr 8 and 0xFF) - MEAN) / STD)
            buffer.putFloat(((pixel and 0xFF) - MEAN) / STD)
        }

        buffer.rewind()
        return buffer
    }

    /**
     * Scales the vector to unit length.
     *
     * Once both sides are unit length, cosine similarity is a plain dot product —
     * which is what [com.securevision.ml.face.match.FaceMatcher] relies on.
     */
    private fun FloatArray.l2Normalised(): FloatArray {
        var sumOfSquares = 0f
        for (value in this) sumOfSquares += value * value

        val magnitude = sqrt(sumOfSquares)
        if (magnitude < MIN_MAGNITUDE) return this

        return FloatArray(size) { index -> this[index] / magnitude }
    }

    private companion object {
        const val TAG = "FaceEmbedder"
        const val MODEL_ASSET = "facenet_512.tflite"
        const val CHANNELS = 3
        const val EXPECTED_INPUT_RANK = 4
        const val MEAN = 127.5f
        const val STD = 128.0f
        const val CPU_THREADS = 4

        /** Below this the vector is all but zero and scaling would amplify noise. */
        const val MIN_MAGNITUDE = 1e-10f
    }
}
