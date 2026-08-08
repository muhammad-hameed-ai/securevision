package com.securevision.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/*
 * SecureVision palette.
 *
 * This file is the only place in the project where a colour literal may appear.
 * Everything else reads `MaterialTheme.colorScheme.*` or `SecureVisionTheme.colors.*`.
 */

// --- Brand ------------------------------------------------------------------

/** Signature cyan. The single accent the whole product is built around. */
internal val BrandCyan = Color(0xFF00C9A7)

/** Darkened cyan, used as the accent on light surfaces where the bright cyan fails contrast. */
internal val BrandCyanDeep = Color(0xFF00806C)

/** Application background in the dark theme. */
internal val NavyBackground = Color(0xFF0A1628)

/** Raised surface in the dark theme. */
internal val NavySurface = Color(0xFF16213A)

// --- Detection semantics ----------------------------------------------------
// Identical in both themes on purpose: these are drawn over the camera image,
// not over an app surface, so they must not shift when the theme changes.

/** A recognised, enrolled person. */
internal val DetectionKnownGreen = Color(0xFF00D97E)

/** An unrecognised face. */
internal val DetectionUnknownRed = Color(0xFFFF3B30)

/** A detected weapon. */
internal val DetectionWeaponOrange = Color(0xFFFF6B00)

/** Movement in an otherwise static scene. */
internal val DetectionMotionAmber = Color(0xFFFFC400)

// --- Material 3 scheme: dark (the product's primary appearance) --------------

internal val SecureVisionDarkColorScheme: ColorScheme = darkColorScheme(
    primary = BrandCyan,
    onPrimary = Color(0xFF00382F),
    primaryContainer = Color(0xFF005247),
    onPrimaryContainer = Color(0xFF6FF7DE),
    inversePrimary = Color(0xFF006B5C),

    secondary = Color(0xFF6FC0B5),
    onSecondary = Color(0xFF073731),
    secondaryContainer = Color(0xFF1C4A44),
    onSecondaryContainer = Color(0xFFB6ECE3),

    tertiary = Color(0xFF7EA6D9),
    onTertiary = Color(0xFF0B2540),
    tertiaryContainer = Color(0xFF1B3A5C),
    onTertiaryContainer = Color(0xFFCFE1F8),

    background = NavyBackground,
    onBackground = Color(0xFFFFFFFF),

    surface = NavySurface,
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF202D48),
    onSurfaceVariant = Color(0xFFA7B4CC),
    surfaceTint = BrandCyan,

    surfaceDim = Color(0xFF0A1628),
    surfaceBright = Color(0xFF2A3A5A),
    surfaceContainerLowest = Color(0xFF070F1C),
    surfaceContainerLow = Color(0xFF101B31),
    surfaceContainer = NavySurface,
    surfaceContainerHigh = Color(0xFF1D2A45),
    surfaceContainerHighest = Color(0xFF253351),

    inverseSurface = Color(0xFFE8EDF7),
    inverseOnSurface = Color(0xFF16213A),

    error = Color(0xFFFF5A50),
    onError = Color(0xFF4A0A06),
    errorContainer = Color(0xFF8C1D16),
    onErrorContainer = Color(0xFFFFDAD6),

    outline = Color(0xFF556482),
    outlineVariant = Color(0xFF313E58),
    scrim = Color(0xFF000000),
)

// --- Material 3 scheme: light -----------------------------------------------

internal val SecureVisionLightColorScheme: ColorScheme = lightColorScheme(
    primary = BrandCyanDeep,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF7FF5DE),
    onPrimaryContainer = Color(0xFF00201A),
    inversePrimary = BrandCyan,

    secondary = Color(0xFF3E6B64),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFC0ECE3),
    onSecondaryContainer = Color(0xFF00201C),

    tertiary = Color(0xFF34577F),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD2E4FF),
    onTertiaryContainer = Color(0xFF0B1D2F),

    background = Color(0xFFF4F7FB),
    onBackground = NavyBackground,

    surface = Color(0xFFFFFFFF),
    onSurface = NavyBackground,
    surfaceVariant = Color(0xFFE3EAF3),
    onSurfaceVariant = Color(0xFF4E5C74),
    surfaceTint = BrandCyanDeep,

    surfaceDim = Color(0xFFDCE3EE),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7FAFE),
    surfaceContainer = Color(0xFFEFF3F9),
    surfaceContainerHigh = Color(0xFFE9EEF6),
    surfaceContainerHighest = Color(0xFFE3E9F3),

    inverseSurface = NavySurface,
    inverseOnSurface = Color(0xFFF0F4FA),

    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),

    outline = Color(0xFF74829B),
    outlineVariant = Color(0xFFC4CEDE),
    scrim = Color(0xFF000000),
)
