package com.securevision.core.ui.preview

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview

/**
 * Renders a preview twice, once per theme.
 *
 * Applied to every component preview in this module so that a colour which only
 * works in the dark theme is caught in the IDE rather than on a device.
 */
@Preview(
    name = "Light",
    group = "themes",
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    showBackground = true,
)
@Preview(
    name = "Dark",
    group = "themes",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
annotation class ThemePreviews
