package com.securevision.feature.live.camera

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NoPhotography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.securevision.core.ui.component.SVEmptyState
import com.securevision.core.ui.component.SVPrimaryButton
import com.securevision.feature.live.R

/**
 * Requests the camera permission, showing a rationale if it is refused.
 *
 * Uses the platform result contract directly rather than adding an accompanist
 * dependency for one permission.
 *
 * The rationale matters here more than in most apps: this is a security product
 * asking for the camera, and an operator who declines deserves an explanation of
 * what it is for rather than a blank screen or a silent retry loop.
 *
 * @param modifier Modifier applied to the rationale state.
 * @param content Rendered once the permission has been granted.
 */
@Composable
fun CameraPermissionGate(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current

    var isGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var hasAsked by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        isGranted = granted
        hasAsked = true
    }

    LaunchedEffect(Unit) {
        if (!isGranted) launcher.launch(Manifest.permission.CAMERA)
    }

    if (isGranted) {
        content()
        return
    }

    SVEmptyState(
        icon = Icons.Outlined.NoPhotography,
        title = stringResource(R.string.live_permission_title),
        subtitle = stringResource(R.string.live_permission_subtitle),
        modifier = modifier.fillMaxSize(),
        action = {
            // Only offered once the system dialog has already been answered;
            // before that the launcher is already on screen and a second button
            // would just be noise.
            if (hasAsked) {
                SVPrimaryButton(
                    text = stringResource(R.string.live_permission_action),
                    onClick = { launcher.launch(Manifest.permission.CAMERA) },
                )
            }
        },
    )
}
