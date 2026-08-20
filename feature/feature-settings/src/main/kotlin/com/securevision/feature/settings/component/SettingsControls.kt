package com.securevision.feature.settings.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import com.securevision.core.ui.theme.SecureVisionDimens

/**
 * A section heading in the settings list.
 *
 * @param text The heading.
 * @param modifier Modifier applied to the heading.
 */
@Composable
fun SettingsSection(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(
            top = SecureVisionDimens.spacingMedium,
            bottom = SecureVisionDimens.spacingExtraSmall,
        ),
    )
}

/**
 * A labelled on/off preference.
 *
 * The whole row is the target, not just the switch — a 32 dp switch is a poor
 * thing to hit on a phone held one-handed.
 *
 * @param title What the setting controls.
 * @param subtitle One line explaining the consequence of turning it off.
 * @param checked Current value.
 * @param onCheckedChange Called with the new value.
 * @param modifier Modifier applied to the row.
 */
@Composable
fun SettingsToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(role = Role.Switch) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onCheckedChange(!checked)
            }
            .padding(vertical = SecureVisionDimens.spacingSmall),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SecureVisionDimens.spacingMedium),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Switch(checked = checked, onCheckedChange = null)
    }
}

/**
 * A labelled slider showing its current value.
 *
 * @param title What the setting controls.
 * @param valueLabel The current value, already formatted.
 * @param value Current position.
 * @param onValueChange Called continuously as the thumb moves.
 * @param valueRange Permitted range.
 * @param steps Discrete stops between the ends, or zero for continuous.
 * @param modifier Modifier applied to the control.
 */
@Composable
fun SettingsSlider(
    title: String,
    valueLabel: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    steps: Int = 0,
) {
    Column(modifier = modifier.padding(vertical = SecureVisionDimens.spacingSmall)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
        )
    }
}

/**
 * One option in a single-choice group.
 *
 * @param label The option.
 * @param selected Whether it is the current choice.
 * @param onSelect Called when chosen.
 * @param modifier Modifier applied to the row.
 */
@Composable
fun SettingsRadioOption(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSelect()
                },
            )
            .padding(vertical = SecureVisionDimens.spacingSmall),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SecureVisionDimens.spacingSmall),
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}

/**
 * A read-only fact.
 *
 * @param label What it is.
 * @param value The fact.
 * @param modifier Modifier applied to the row.
 */
@Composable
fun SettingsInfoRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = SecureVisionDimens.spacingSmall),
        horizontalArrangement = Arrangement.spacedBy(SecureVisionDimens.spacingMedium),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}
