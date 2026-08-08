package com.securevision.core.data.storage

import android.content.Context
import android.graphics.Bitmap
import androidx.core.net.toUri
import com.securevision.core.common.Constants
import com.securevision.core.common.dispatcher.DispatcherProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.withContext

/**
 * Reads and writes the app's private files.
 *
 * Everything lands under [Context.getFilesDir]. Nothing is written to external
 * storage or MediaStore, and that is a product requirement rather than a
 * preference: enrolment photos and alert snapshots are images of people's faces,
 * and anything in shared storage is readable by other apps and picked up by the
 * gallery.
 *
 * All file I/O is confined to the injected IO dispatcher.
 *
 * @property context Application context, used only for its private files directory.
 * @property dispatcherProvider Supplies the IO dispatcher.
 */
@Singleton
class InternalStorageManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dispatcherProvider: DispatcherProvider,
) {

    /**
     * Writes an alert snapshot as JPEG.
     *
     * @param bitmap Frame to persist.
     * @return `file://` URI of the written image.
     */
    suspend fun saveSnapshot(bitmap: Bitmap): String =
        saveImage(bitmap, Constants.Storage.SNAPSHOT_DIRECTORY, SNAPSHOT_PREFIX)

    /**
     * Writes an enrolment photo as JPEG.
     *
     * Only ever displayed — recognition runs against the stored embedding, never
     * against this file.
     *
     * @param bitmap Photo to persist.
     * @return `file://` URI of the written image.
     */
    suspend fun saveProfilePhoto(bitmap: Bitmap): String =
        saveImage(bitmap, Constants.Storage.PROFILE_PHOTO_DIRECTORY, PROFILE_PREFIX)

    /**
     * The directory clips are recorded into, created if absent.
     *
     * Returned as a [File] because CameraX's recorder needs a real path to open,
     * not a URI.
     */
    suspend fun getRecordingsDirectory(): File = withContext(dispatcherProvider.io) {
        directory(Constants.Storage.RECORDING_DIRECTORY)
    }

    /**
     * Allocates a uniquely named, not-yet-created file for a new recording.
     *
     * @return Destination for the recorder to write to.
     */
    suspend fun createRecordingFile(): File = withContext(dispatcherProvider.io) {
        File(
            directory(Constants.Storage.RECORDING_DIRECTORY),
            fileName(RECORDING_PREFIX, Constants.Storage.VIDEO_EXTENSION),
        )
    }

    /**
     * Deletes a file previously produced by this manager.
     *
     * Refuses to touch anything outside the app's private files directory, so a
     * malformed or hostile URI cannot be used to delete arbitrary paths.
     *
     * @param uri `file://` URI or absolute path.
     * @return `true` if a file was deleted.
     */
    suspend fun deleteFile(uri: String): Boolean = withContext(dispatcherProvider.io) {
        val path = runCatching { uri.toUri().path }.getOrNull() ?: uri
        val target = File(path)

        if (!target.canonicalPath.startsWith(context.filesDir.canonicalPath)) {
            return@withContext false
        }

        target.exists() && target.delete()
    }

    /** Total bytes currently held across snapshots, profile photos and recordings. */
    suspend fun usedBytes(): Long = withContext(dispatcherProvider.io) {
        listOf(
            Constants.Storage.SNAPSHOT_DIRECTORY,
            Constants.Storage.PROFILE_PHOTO_DIRECTORY,
            Constants.Storage.RECORDING_DIRECTORY,
        ).sumOf { name ->
            directory(name).walkTopDown().filter(File::isFile).sumOf(File::length)
        }
    }

    private suspend fun saveImage(
        bitmap: Bitmap,
        directoryName: String,
        prefix: String,
    ): String = withContext(dispatcherProvider.io) {
        val target = File(
            directory(directoryName),
            fileName(prefix, Constants.Storage.IMAGE_EXTENSION),
        )

        target.outputStream().use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, Constants.Storage.JPEG_QUALITY, stream)
        }

        target.toURI().toString()
    }

    private fun directory(name: String): File =
        File(context.filesDir, name).apply { if (!exists()) mkdirs() }

    /** Sortable, collision-resistant name: a timestamp plus a short random suffix. */
    private fun fileName(prefix: String, extension: String): String {
        val stamp = STAMP_FORMATTER.format(Instant.now())
        val suffix = (RANDOM_SUFFIX_RANGE).random()

        return "${prefix}_${stamp}_$suffix.$extension"
    }

    private companion object {
        const val SNAPSHOT_PREFIX = "snapshot"
        const val PROFILE_PREFIX = "profile"
        const val RECORDING_PREFIX = "recording"

        val RANDOM_SUFFIX_RANGE = 1000..9999

        val STAMP_FORMATTER: DateTimeFormatter = DateTimeFormatter
            .ofPattern(Constants.DateTime.FILE_STAMP_PATTERN, Locale.ROOT)
            .withZone(ZoneId.systemDefault())
    }
}
