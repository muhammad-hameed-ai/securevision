package com.securevision.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Carries the detection palette down the tree.
 *
 * `static` because these values never change within a single theme instance —
 * a static local skips invalidation tracking entirely.
 */
internal val LocalSecureVisionColors = staticCompositionLocalOf<SecureVisionColors> {
    error("SecureVisionColors requested outside of SecureVisionTheme.")
}

/**
 * The SecureVision Material 3 theme.
 *
 * Wraps every screen in the app. Dynamic colour is deliberately not supported:
 * the detection colours carry meaning that must not be re-tinted by a device
 * wallpaper.
 *
 * @param darkTheme Whether to use the dark palette; follows the system by default.
 * @param content Content to theme.
 */
@Composable
fun SecureVisionTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) SecureVisionDarkColorScheme else SecureVisionLightColorScheme
    val detectionColors =
        if (darkTheme) SecureVisionDarkDetectionColors else SecureVisionLightDetectionColors

    CompositionLocalProvider(LocalSecureVisionColors provides detectionColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SecureVisionTypography,
            shapes = SecureVisionShapes,
            content = content,
        )
    }
}

/**
 * Accessors for SecureVision's own tokens, mirroring how `MaterialTheme` exposes
 * the Material ones.
 */
object SecureVisionTheme {

    /** The detection palette for the current theme. */
    val colors: SecureVisionColors
        @Composable
        @ReadOnlyComposable
        get() = LocalSecureVisionColors.current

    /** The spacing and sizing scale. */
    val dimens: SecureVisionDimens
        get() = SecureVisionDimens
}
