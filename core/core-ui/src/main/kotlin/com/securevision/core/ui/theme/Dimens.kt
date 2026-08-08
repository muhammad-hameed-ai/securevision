package com.securevision.core.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Spacing and sizing tokens.
 *
 * A four-point scale. Components reference these rather than literal `dp`
 * values so that vertical rhythm stays consistent across screens written by
 * different hands.
 */
object SecureVisionDimens {

    // --- Spacing scale -------------------------------------------------------

    /** 4 dp — hairline gaps, icon-to-label spacing inside a badge. */
    val spacingExtraSmall: Dp = 4.dp

    /** 8 dp — spacing between tightly related elements. */
    val spacingSmall: Dp = 8.dp

    /** 12 dp — internal padding of compact components. */
    val spacingMediumSmall: Dp = 12.dp

    /** 16 dp — the default gutter and card padding. */
    val spacingMedium: Dp = 16.dp

    /** 24 dp — separation between sections. */
    val spacingLarge: Dp = 24.dp

    /** 32 dp — separation between major blocks. */
    val spacingExtraLarge: Dp = 32.dp

    /** 48 dp — breathing room around an empty state. */
    val spacingHuge: Dp = 48.dp

    // --- Component sizing ----------------------------------------------------

    /** Minimum touch target, matching the platform accessibility guideline. */
    val minTouchTarget: Dp = 48.dp

    /** Height of a primary button. */
    val buttonHeight: Dp = 52.dp

    /** Diameter of the spinner shown inside a loading button. */
    val buttonProgressSize: Dp = 20.dp

    /** Stroke width of the spinner shown inside a loading button. */
    val buttonProgressStroke: Dp = 2.dp

    /** Leading icon size inside cards and list rows. */
    val iconSmall: Dp = 20.dp

    /** Standard icon size. */
    val iconMedium: Dp = 24.dp

    /** Illustration-scale icon used by empty states. */
    val iconLarge: Dp = 64.dp

    /** Resting elevation of a card. */
    val cardElevation: Dp = 2.dp

    /** Stroke width of a detection overlay box. */
    val overlayStroke: Dp = 3.dp
}
