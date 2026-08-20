package com.securevision.feature.alerts.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.securevision.core.model.AgeBand
import com.securevision.feature.alerts.R

/**
 * Shows an estimated age, or admits there isn't one.
 *
 * `null` means no age model examined this face, and it renders "Age: unknown" —
 * italicised and muted, exactly like the beard and mask chips, so it cannot be
 * skim-read as a value. **No age is inferred from anything else.** For an
 * unrecognised person the age band is often the only description the operator
 * gets, which is precisely why inventing one would be worse than leaving it
 * blank.
 *
 * @param estimatedAge Years, or `null` when not assessed.
 * @param modifier Modifier applied to the chip.
 */
@Composable
fun AgeChip(estimatedAge: Int?, modifier: Modifier = Modifier) {
    val band = AgeBand.of(estimatedAge)

    val text = if (estimatedAge == null || band == null) {
        stringResource(R.string.alerts_age_unknown)
    } else {
        stringResource(R.string.alerts_age_estimate, estimatedAge, stringResource(band.labelRes()))
    }

    Text(
        text = text,
        style = if (band == null) {
            MaterialTheme.typography.labelSmall.copy(fontStyle = FontStyle.Italic)
        } else {
            MaterialTheme.typography.labelSmall
        },
        color = if (band == null) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        modifier = modifier
            .clip(MaterialTheme.shapes.extraSmall)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

/** Display name for an age band. */
internal fun AgeBand.labelRes(): Int = when (this) {
    AgeBand.CHILD -> R.string.alerts_age_child
    AgeBand.TEEN -> R.string.alerts_age_teen
    AgeBand.YOUNG_ADULT -> R.string.alerts_age_young_adult
    AgeBand.ADULT -> R.string.alerts_age_adult
    AgeBand.SENIOR -> R.string.alerts_age_senior
}
