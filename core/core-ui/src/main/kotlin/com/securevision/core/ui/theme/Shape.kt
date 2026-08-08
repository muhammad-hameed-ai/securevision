package com.securevision.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Corner radii for SecureVision.
 *
 * Two families: buttons and chips are the tighter 8/12 dp pair, cards and sheets
 * the softer 12/16 dp pair, so a control never reads as a container.
 */
val SecureVisionShapes: Shapes = Shapes(
    /** Badges and other small inline markers. */
    extraSmall = RoundedCornerShape(4.dp),

    /** Chips and compact buttons. */
    small = RoundedCornerShape(8.dp),

    /** Standard buttons and text fields. */
    medium = RoundedCornerShape(12.dp),

    /** Cards and list containers. */
    large = RoundedCornerShape(16.dp),

    /** Bottom sheets and dialogs. */
    extraLarge = RoundedCornerShape(24.dp),
)
