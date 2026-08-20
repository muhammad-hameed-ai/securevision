package com.securevision.feature.recordings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.SubcomposeAsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import com.securevision.core.model.Recording
import com.securevision.core.ui.component.SVCard
import com.securevision.core.ui.component.SVEmptyState
import com.securevision.core.ui.component.SVTopBar
import com.securevision.core.common.extension.toDurationLabel
import com.securevision.core.common.extension.toFormattedDateTime
import com.securevision.core.ui.theme.SecureVisionDimens

/**
 * The recordings gallery.
 *
 * States plainly that clips hold the camera feed without detection boxes. That
 * sentence is the honest counterpart to a limitation the UI cannot otherwise
 * show: someone reviewing a clip would reasonably assume the overlay they saw
 * while recording was captured too.
 *
 * @param uiState What to render.
 * @param pendingDeletion Clip awaiting delete confirmation, if any.
 * @param onPlay Opens a clip.
 * @param onDeleteRequested Asks to remove a clip.
 * @param onDeleteConfirmed Confirms removal.
 * @param onDeleteCancelled Abandons removal.
 * @param modifier Modifier applied to the screen.
 */
@Composable
fun RecordingsScreen(
    uiState: RecordingsUiState,
    pendingDeletion: Recording?,
    onPlay: (String) -> Unit,
    onDeleteRequested: (Recording) -> Unit,
    onDeleteConfirmed: () -> Unit,
    onDeleteCancelled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { SVTopBar(title = stringResource(R.string.recordings_title)) },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (uiState) {
                RecordingsUiState.Loading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary,
                )

                RecordingsUiState.Empty -> SVEmptyState(
                    icon = Icons.Outlined.VideoLibrary,
                    title = stringResource(R.string.recordings_empty_title),
                    subtitle = stringResource(R.string.recordings_empty_subtitle),
                    modifier = Modifier.fillMaxSize(),
                )

                is RecordingsUiState.Content -> Column {
                    Text(
                        text = stringResource(R.string.recordings_no_overlay_notice),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(
                            horizontal = SecureVisionDimens.spacingMedium,
                            vertical = SecureVisionDimens.spacingSmall,
                        ),
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = CELL_MIN_WIDTH),
                        contentPadding = PaddingValues(SecureVisionDimens.spacingMedium),
                        horizontalArrangement = Arrangement.spacedBy(
                            SecureVisionDimens.spacingSmall,
                        ),
                        verticalArrangement = Arrangement.spacedBy(SecureVisionDimens.spacingSmall),
                    ) {
                        items(items = uiState.recordings, key = Recording::id) { recording ->
                            RecordingCell(
                                recording = recording,
                                onPlay = { onPlay(recording.id) },
                                onDelete = { onDeleteRequested(recording) },
                            )
                        }
                    }
                }
            }
        }
    }

    if (pendingDeletion != null) {
        AlertDialog(
            onDismissRequest = onDeleteCancelled,
            title = { Text(stringResource(R.string.recordings_delete_title)) },
            text = { Text(stringResource(R.string.recordings_delete_body)) },
            confirmButton = {
                TextButton(onClick = onDeleteConfirmed) {
                    Text(
                        text = stringResource(R.string.recordings_delete_confirm),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = onDeleteCancelled) {
                    Text(stringResource(R.string.recordings_cancel))
                }
            },
        )
    }
}

@Composable
private fun RecordingCell(
    recording: Recording,
    onPlay: () -> Unit,
    onDelete: () -> Unit,
) {
    SVCard(
        onClick = onPlay,
        contentPadding = PaddingValues(SecureVisionDimens.spacingSmall),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(THUMBNAIL_ASPECT)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            VideoThumbnail(filePath = recording.filePath)

            Icon(
                imageVector = Icons.Outlined.PlayCircleOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = recording.durationMs.toDurationLabel(),
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    text = recording.createdAt.toFormattedDateTime(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = stringResource(R.string.recordings_delete_confirm),
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(SecureVisionDimens.iconSmall),
                )
            }
        }
    }
}

/**
 * A poster frame decoded straight from the clip.
 *
 * Coil's video decoder reads the first frame on demand, so no thumbnail file is
 * generated or stored. That keeps a second copy of every recording off a device
 * whose storage is already the scarcest thing this feature consumes.
 */
@Composable
private fun VideoThumbnail(filePath: String) {
    val context = LocalContext.current

    val loader = remember(context) {
        ImageLoader.Builder(context)
            .components { add(VideoFrameDecoder.Factory()) }
            .build()
    }

    SubcomposeAsyncImage(
        model = ImageRequest.Builder(context).data(filePath).build(),
        imageLoader = loader,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
        error = {},
    )
}

private val CELL_MIN_WIDTH = 160.dp
private const val THUMBNAIL_ASPECT = 16f / 9f
