package com.securevision.feature.live.camera

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Asks once for permission to post notifications.
 *
 * Deliberately **not** a gate, unlike [CameraPermissionGate]. The camera is what
 * the screen is for, so refusing it leaves nothing to show; notifications are an
 * enhancement, and refusing them must leave detection, recording and the in-app
 * alert list completely intact. A user who says no gets a quieter app, not a
 * broken one.
 *
 * Asks a single time per screen entry. Re-prompting on every alert would be the
 * behaviour of an app that has not accepted the answer.
 *
 * @param onResult Called with the outcome, including on API levels below 33 where
 *   the permission does not exist and posting is granted at install time.
 */
@Composable
fun NotificationPermissionRequest(onResult: (granted: Boolean) -> Unit = {}) {
    val context = LocalContext.current

    val alreadyResolved = remember {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            true
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    var hasAsked by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = onResult,
    )

    LaunchedEffect(Unit) {
        if (alreadyResolved) {
            onResult(true)
            return@LaunchedEffect
        }
        if (hasAsked) return@LaunchedEffect

        hasAsked = true
        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
