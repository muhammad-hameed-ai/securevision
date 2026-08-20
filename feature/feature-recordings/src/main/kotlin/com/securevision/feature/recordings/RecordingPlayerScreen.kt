package com.securevision.feature.recordings

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.securevision.core.ui.component.SVEmptyState
import com.securevision.core.ui.component.SVTopBar
import java.io.File

/**
 * Plays one clip.
 *
 * The player is released in `onDispose` rather than left to garbage collection:
 * ExoPlayer holds a codec, and leaking one blocks video playback everywhere on
 * the device until the process dies.
 *
 * @param filePath Absolute path of the clip, or `null` if it has been deleted.
 * @param isLoading Whether the lookup is still running, so a good clip is never
 *   reported missing while it is still being found.
 * @param onBack Returns to the gallery.
 * @param modifier Modifier applied to the screen.
 */
@Composable
fun RecordingPlayerScreen(
    filePath: String?,
    isLoading: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            SVTopBar(title = stringResource(R.string.recordings_player_title), onBack = onBack)
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary,
                )
                return@Box
            }

            val file = filePath?.let(::File)

            if (file == null || !file.exists()) {
                SVEmptyState(
                    icon = Icons.Outlined.ErrorOutline,
                    title = stringResource(R.string.recordings_missing_title),
                    subtitle = stringResource(R.string.recordings_missing_subtitle),
                    modifier = Modifier.fillMaxSize(),
                )
                return@Box
            }

            VideoSurface(file = file)
        }
    }
}

@Composable
private fun VideoSurface(file: File) {
    val context = LocalContext.current

    val player = remember(file.absolutePath) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    AndroidView(
        factory = { viewContext ->
            PlayerView(viewContext).apply {
                this.player = player
                useController = true
            }
        },
        modifier = Modifier.fillMaxSize(),
    )
}
