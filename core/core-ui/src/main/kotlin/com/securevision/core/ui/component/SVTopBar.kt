package com.securevision.core.ui.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.securevision.core.ui.R
import com.securevision.core.ui.preview.PreviewContainer
import com.securevision.core.ui.preview.ThemePreviews

/**
 * The standard SecureVision app bar.
 *
 * @param title Screen title.
 * @param modifier Modifier applied to the bar.
 * @param onBack Invoked when the up arrow is pressed. Pass `null` on a top-level
 *   destination, where there is nothing to navigate up to.
 * @param navigationIcon Replaces the up arrow entirely — used by the app shell to
 *   show a drawer menu button on top-level destinations. Takes precedence over
 *   [onBack] when both are supplied.
 * @param actions Trailing controls, laid out at the end of the bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SVTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    CenterAlignedTopAppBar(
        modifier = modifier,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        navigationIcon = {
            when {
                navigationIcon != null -> navigationIcon()
                onBack != null -> IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.sv_content_description_back),
                    )
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            navigationIconContentColor = MaterialTheme.colorScheme.primary,
            actionIconContentColor = MaterialTheme.colorScheme.primary,
        ),
    )
}

@ThemePreviews
@Composable
private fun SVTopBarPreview() {
    PreviewContainer {
        SVTopBar(title = "Alerts", onBack = {})
        SVTopBar(title = "Dashboard")
    }
}
