package com.securevision.feature.live.camera

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaMetadataRetriever
import android.util.Log
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import java.io.File
import java.util.UUID

/**
 * Drives CameraX video capture for the live screen.
 *
 * **Records the camera feed only. Detection boxes are not in the file.** CameraX
 * writes the sensor stream; the overlay is a Compose layer above `PreviewView`,
 * and no CameraX API composites one into the other. Burning the boxes in needs a
 * custom OpenGL pipeline feeding `MediaCodec` — real work, deliberately deferred.
 * The gallery states this rather than letting anyone assume otherwise.
 *
 * **Silent by design.** `withAudioEnabled` is never called, so no
 * `RECORD_AUDIO` permission is needed and none is declared.
 */
class VideoRecorder(
    private val context: Context,
) {

    /** The CameraX use case, bound by [CameraPreview]. */
    val videoCapture: VideoCapture<Recorder> = VideoCapture.withOutput(
        Recorder.Builder()
            // HD rather than the highest available: a security clip is watched for
            // what happened, not for fidelity, and UHD fills internal storage in
            // minutes on a device with no SD card to fall back on.
            .setQualitySelector(QualitySelector.from(Quality.HD))
            .build(),
    )

    private var activeRecording: Recording? = null
    private var startedAtMillis = 0L
    private var outputFile: File? = null

    /** Whether a clip is currently being written. */
    val isRecording: Boolean get() = activeRecording != null

    /** Milliseconds since the current clip started, or zero when idle. */
    fun elapsedMillis(): Long =
        if (isRecording) System.currentTimeMillis() - startedAtMillis else 0L

    /**
     * Begins recording to [destination].
     *
     * Audio is deliberately not enabled, so this needs no runtime permission —
     * which is why the call is annotated rather than guarded.
     *
     * @param destination File to write, already allocated in internal storage.
     * @param onFinished Called with the completed clip's metadata, or `null` if
     *   the recording failed. Always called exactly once per successful [start].
     */
    @SuppressLint("MissingPermission")
    fun start(destination: File, onFinished: (com.securevision.core.model.Recording?) -> Unit) {
        if (isRecording) return

        outputFile = destination
        startedAtMillis = System.currentTimeMillis()

        val options = FileOutputOptions.Builder(destination).build()

        activeRecording = runCatching {
            videoCapture.output
                .prepareRecording(context, options)
                .start(ContextCompat.getMainExecutor(context)) { event ->
                    if (event is VideoRecordEvent.Finalize) {
                        onFinished(finalise(event, destination))
                    }
                }
        }.onFailure { throwable ->
            Log.e(TAG, "could not start recording", throwable)
            reset()
            onFinished(null)
        }.getOrNull()
    }

    /** Stops the current clip. The finish callback fires when the file is closed. */
    fun stop() {
        activeRecording?.stop()
        activeRecording = null
    }

    private fun finalise(
        event: VideoRecordEvent.Finalize,
        destination: File,
    ): com.securevision.core.model.Recording? {
        val elapsed = System.currentTimeMillis() - startedAtMillis
        reset()

        if (event.hasError()) {
            Log.e(TAG, "recording finalised with error ${event.error}", event.cause)
            // A partial file is worse than none: it appears in the gallery and
            // fails to open. Remove it rather than record a broken row.
            destination.delete()
            return null
        }

        if (!destination.exists() || destination.length() == 0L) {
            Log.e(TAG, "recording produced no data")
            destination.delete()
            return null
        }

        return com.securevision.core.model.Recording(
            id = UUID.randomUUID().toString(),
            filePath = destination.absolutePath,
            // Prefer the container's own duration; wall-clock is a fallback for
            // devices whose metadata is missing.
            durationMs = destination.durationMillis() ?: elapsed,
            thumbnailUri = null,
            createdAt = startedAtMillisOrNow(),
        )
    }

    private fun startedAtMillisOrNow(): Long =
        if (startedAtMillis > 0L) startedAtMillis else System.currentTimeMillis()

    private fun reset() {
        activeRecording = null
        outputFile = null
    }

    private companion object {
        const val TAG = "VideoRecorder"
    }
}

/**
 * Reads a clip's duration from its container.
 *
 * @return Duration in milliseconds, or `null` when the file has no readable
 *   metadata — which happens on a truncated write.
 */
internal fun File.durationMillis(): Long? = runCatching {
    // try/finally rather than `use`: MediaMetadataRetriever only became
    // AutoCloseable at API 29, and this app supports 26.
    val retriever = MediaMetadataRetriever()

    try {
        retriever.setDataSource(absolutePath)
        retriever
            .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            ?.toLongOrNull()
    } finally {
        retriever.release()
    }
}.getOrNull()
