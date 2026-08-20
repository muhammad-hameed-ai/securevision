package com.securevision.ml.weapon.detect

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.util.Log
import com.securevision.core.domain.engine.EngineStatus
import com.securevision.core.domain.engine.InferenceDelegate
import com.securevision.core.model.WeaponDetection
import dagger.hilt.android.qualifiers.ApplicationContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import javax.inject.Inject
import javax.inject.Singleton
import org.tensorflow.lite.Delegate
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.nnapi.NnApiDelegate

/**
 * TFLite weapon detection.
 *
 * Built to activate the moment a model lands at [MODEL_ASSET]; until then it
 * reports [EngineStatus.RecognitionUnavailable] and returns no detections, so
 * faces and motion keep working and the live screen can say why weapons do not.
 *
 * Shapes are read from the model rather than assumed, and a model whose class
 * count disagrees with [WeaponClassMap] is refused outright. Accepting it would
 * label a knife as a rifle — a mislabelling that looks like a working detector.
 */
@Singleton
class WeaponDetector @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    private var interpreter: Interpreter? = null
    private var delegate: Delegate? = null
    private var inputSize: Int = 0

    /**
     * Whether the loaded model wants NCHW rather than NHWC.
     *
     * Read from the input tensor at load, never assumed: a PyTorch-exported
     * YOLOv8 is channels-first and a TFLite-converted one is channels-last, and
     * feeding one the other's byte order fails silently.
     */
    private var inputIsChannelsFirst: Boolean = false
    private var outputShape: IntArray = IntArray(0)

    /** Current readiness. */
    var status: EngineStatus = EngineStatus.Initialising
        private set

    /** Whether [detect] can produce results. */
    val isReady: Boolean get() = interpreter != null

    /** Loads the model, selecting the fastest available delegate. */
    @Synchronized
    fun load() {
        if (interpreter != null) return

        val model = readModelAsset()
        if (model == null) {
            Log.w(TAG, "$MODEL_ASSET not found — weapon detection disabled")
            status = EngineStatus.RecognitionUnavailable(
                EngineStatus.RecognitionUnavailable.Reason.MODEL_NOT_INSTALLED,
            )
            return
        }

        val (created, activeDelegate, createdDelegate) = createInterpreter(model)
        if (created == null) {
            status = EngineStatus.RecognitionUnavailable(
                EngineStatus.RecognitionUnavailable.Reason.MODEL_LOAD_FAILED,
            )
            return
        }

        val input = created.getInputTensor(0).shape()
        val output = created.getOutputTensor(0).shape()

        Log.i(TAG, "model loaded: $MODEL_ASSET")
        Log.i(TAG, "  input  shape = ${input.joinToString()}")
        Log.i(TAG, "  output shape = ${output.joinToString()}")
        Log.i(TAG, "  delegate = $activeDelegate")
        Log.i(TAG, "  expecting ${WeaponClassMap.EXPECTED_CLASS_COUNT} classes: " +
            WeaponClassMap.LABELS.joinToString())

        val layout = WeaponOutputParser.Layout.from(output, WeaponOutputParser.expectedChannels())
        if (layout == null) {
            Log.e(
                TAG,
                "output ${output.joinToString()} has no axis of " +
                    "${WeaponOutputParser.expectedChannels()} channels, so the model does not " +
                    "emit WeaponClassMap's ${WeaponClassMap.EXPECTED_CLASS_COUNT} classes " +
                    "(${WeaponClassMap.LABELS.joinToString()}) — refusing to load rather than " +
                    "mislabel detections",
            )
            created.close()
            createdDelegate?.close()
            status = EngineStatus.RecognitionUnavailable(
                EngineStatus.RecognitionUnavailable.Reason.MODEL_LOAD_FAILED,
            )
            return
        }

        interpreter = created
        delegate = createdDelegate

        // Detected, not assumed. A YOLOv8 export straight from PyTorch is
        // channels-first [1,3,640,640]; a TFLite-converted one is usually
        // channels-last [1,640,640,3]. Reading the size off a fixed axis picks up
        // the channel count (3) from an NCHW model and tries to letterbox to 3×3.
        inputIsChannelsFirst = input.getOrNull(1) == CHANNELS && input.size == INPUT_RANK
        inputSize = if (inputIsChannelsFirst) {
            input.getOrElse(2) { DEFAULT_INPUT_SIZE }
        } else {
            input.getOrElse(1) { DEFAULT_INPUT_SIZE }
        }

        Log.i(
            TAG,
            "  input layout = ${if (inputIsChannelsFirst) "NCHW" else "NHWC"}, size = $inputSize",
        )

        outputShape = output
        status = EngineStatus.Ready(
            embeddingDimensions = layout.classCount,
            delegate = activeDelegate,
        )
    }

    /**
     * Detects weapons in a frame.
     *
     * @param frame The upright frame.
     * @param confidenceThreshold Minimum class score.
     * @return Detections in frame-normalised coordinates, suppressed and sorted.
     */
    fun detect(frame: Bitmap, confidenceThreshold: Float): List<WeaponDetection> {
        val activeInterpreter = interpreter ?: return emptyList()

        return runCatching {
            val letterbox = Letterbox(frame.width, frame.height, inputSize)
            val input = frame.toLetterboxedInput(letterbox)

            val flatSize = outputShape.fold(1) { total, dimension -> total * dimension }
            val output = Array(1) {
                Array(outputShape[1]) { FloatArray(outputShape[2]) }
            }

            activeInterpreter.run(input, output)

            val flattened = FloatArray(flatSize)
            var cursor = 0
            for (row in output[0]) {
                row.copyInto(flattened, cursor)
                cursor += row.size
            }

            val raw = WeaponOutputParser.parse(
                output = flattened,
                shape = outputShape,
                confidenceThreshold = confidenceThreshold,
                inputSize = inputSize,
            )

            // Undo the padding before suppression, so IoU is computed in the space
            // the boxes will actually be drawn in.
            val corrected = raw.map { detection ->
                detection.copy(boundingBox = letterbox.toFrameSpace(detection.boundingBox))
            }

            NonMaxSuppression.apply(corrected)
        }.onFailure { throwable ->
            Log.e(TAG, "inference failed", throwable)
        }.getOrDefault(emptyList())
    }

    /** Releases the interpreter and any delegate. */
    @Synchronized
    fun close() {
        runCatching { interpreter?.close() }
        runCatching { delegate?.close() }
        interpreter = null
        delegate = null
        status = EngineStatus.Initialising
    }

    /** Scales into the square input preserving aspect, padding the remainder grey. */
    private fun Bitmap.toLetterboxedInput(letterbox: Letterbox): ByteBuffer {
        val canvasBitmap = Bitmap.createBitmap(inputSize, inputSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(canvasBitmap)
        canvas.drawColor(PAD_COLOUR)

        val matrix = Matrix().apply {
            postScale(letterbox.scale, letterbox.scale)
            postTranslate(letterbox.padX, letterbox.padY)
        }
        canvas.drawBitmap(this, matrix, FILTER_PAINT)

        val buffer = ByteBuffer
            .allocateDirect(inputSize * inputSize * CHANNELS * Float.SIZE_BYTES)
            .order(ByteOrder.nativeOrder())

        val pixels = IntArray(inputSize * inputSize)
        canvasBitmap.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)
        canvasBitmap.recycle()

        // YOLO expects 0..1, not the -1..1 FaceNet uses. Getting this wrong does
        // not throw; it just makes the detector see an image it was never trained
        // on. The same is true of channel order — feeding interleaved pixels to a
        // channels-first model produces silence, not an error.
        writePixels(pixels, buffer)

        buffer.rewind()
        return buffer
    }

    /**
     * Writes normalised pixels in whichever channel order the model expects.
     *
     * NHWC interleaves per pixel — R,G,B,R,G,B — while NCHW writes three whole
     * planes: every red value, then every green, then every blue. Visiting the
     * pixel array three times for NCHW is deliberate; building three float arrays
     * first would allocate 4.9 MB per frame at 640×640, on a path that runs
     * several times a second.
     *
     * @param pixels ARGB pixels of the letterboxed frame.
     * @param buffer Destination, positioned at zero.
     */
    private fun writePixels(pixels: IntArray, buffer: ByteBuffer) {
        if (!inputIsChannelsFirst) {
            for (pixel in pixels) {
                buffer.putFloat(((pixel shr RED_SHIFT) and CHANNEL_MASK) / MAX_CHANNEL)
                buffer.putFloat(((pixel shr GREEN_SHIFT) and CHANNEL_MASK) / MAX_CHANNEL)
                buffer.putFloat((pixel and CHANNEL_MASK) / MAX_CHANNEL)
            }
            return
        }

        for (pixel in pixels) {
            buffer.putFloat(((pixel shr RED_SHIFT) and CHANNEL_MASK) / MAX_CHANNEL)
        }
        for (pixel in pixels) {
            buffer.putFloat(((pixel shr GREEN_SHIFT) and CHANNEL_MASK) / MAX_CHANNEL)
        }
        for (pixel in pixels) {
            buffer.putFloat((pixel and CHANNEL_MASK) / MAX_CHANNEL)
        }
    }

    private fun createInterpreter(
        model: MappedByteBuffer,
    ): Triple<Interpreter?, InferenceDelegate, Delegate?> {
        val compatibility = runCatching { CompatibilityList() }.getOrNull()

        if (compatibility?.isDelegateSupportedOnThisDevice == true) {
            runCatching {
                val gpu = GpuDelegate(compatibility.bestOptionsForThisDevice)
                Interpreter(model, Interpreter.Options().addDelegate(gpu)) to gpu
            }.getOrNull()?.let { (created, gpu) ->
                return Triple(created, InferenceDelegate.GPU, gpu)
            }
            Log.w(TAG, "GPU delegate unavailable, trying NNAPI")
        }

        runCatching {
            val nnApi = NnApiDelegate()
            Interpreter(model, Interpreter.Options().addDelegate(nnApi)) to nnApi
        }.getOrNull()?.let { (created, nnApi) ->
            return Triple(created, InferenceDelegate.NNAPI, nnApi)
        }
        Log.w(TAG, "NNAPI delegate unavailable, falling back to CPU")

        val cpu = runCatching {
            Interpreter(model, Interpreter.Options().setNumThreads(CPU_THREADS))
        }.onFailure { Log.e(TAG, "CPU interpreter failed", it) }.getOrNull()

        return Triple(cpu, InferenceDelegate.CPU, null)
    }

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
    }.getOrNull()

    private companion object {
        const val TAG = "WeaponDetector"
        const val MODEL_ASSET = "weapon_detector.tflite"
        const val CHANNELS = 3
        const val CPU_THREADS = 4
        const val DEFAULT_INPUT_SIZE = 640
        const val MAX_CHANNEL = 255f

        /** `[batch, …, …, …]` — any input tensor this detector understands. */
        const val INPUT_RANK = 4

        const val RED_SHIFT = 16
        const val GREEN_SHIFT = 8
        const val CHANNEL_MASK = 0xFF

        /** Neutral grey padding — black or white would read as a strong edge. */
        val PAD_COLOUR = Color.rgb(114, 114, 114)

        val FILTER_PAINT = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    }
}
