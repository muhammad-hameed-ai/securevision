package com.securevision.feature.alerts.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.securevision.feature.alerts.R

/**
 * Renders one soft attribute, distinguishing "no" from "nobody looked".
 *
 * **`null` means not assessed and must never render as "No".** The attribute
 * classifiers are not installed yet, so every stored alert currently holds `null`
 * for beard and mask. Showing "No beard" would be the app asserting something no
 * model ever examined — a false statement in an audit record that a person may
 * later rely on.
 *
 * The three states are visually distinct as well as textually: unknown is muted
 * and italicised, so it cannot be skim-read as a negative.
 *
 * @param label What the attribute is, e.g. "Beard".
 * @param value `true`, `false`, or `null` for not assessed.
 * @param modifier Modifier applied to the chip.
 */
@Composable
fun AttributeChip(
    label: String,
    value: Boolean?,
    modifier: Modifier = Modifier,
) {
    val text = when (value) {
        true -> stringResource(R.string.alerts_attribute_yes, label)
        false -> stringResource(R.string.alerts_attribute_no, label)
        null -> stringResource(R.string.alerts_attribute_unknown, label)
    }

    val colour = when (value) {
        null -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurface
    }

    Text(
        text = text,
        style = if (value == null) {
            MaterialTheme.typography.labelSmall.copy(
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
            )
        } else {
            MaterialTheme.typography.labelSmall
        },
        color = colour,
        modifier = modifier
            .clip(MaterialTheme.shapes.extraSmall)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}
