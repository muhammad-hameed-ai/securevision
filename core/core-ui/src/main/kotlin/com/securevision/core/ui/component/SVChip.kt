package com.securevision.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.securevision.core.ui.preview.PreviewContainer
import com.securevision.core.ui.preview.ThemePreviews
import com.securevision.core.ui.theme.SecureVisionDimens

/**
 * A single-choice filter chip, used to narrow the alert and history lists.
 *
 * @param label Chip text.
 * @param selected Whether this chip is the active filter.
 * @param onClick Invoked on tap.
 * @param modifier Modifier applied to the chip.
 * @param enabled Whether the chip can be selected.
 */
@Composable
fun SVChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        label = { Text(text = label, style = MaterialTheme.typography.labelMedium) },
        leadingIcon = if (selected) {
            {
                Icon(
                    imageVector = Icons.Filled.Check,
                    // Decorative: selection is already exposed through chip semantics.
                    contentDescription = null,
                    modifier = Modifier.size(SecureVisionDimens.iconSmall),
                )
            }
        } else {
            null
        },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = enabled,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.outlineVariant,
            selectedBorderColor = MaterialTheme.colorScheme.primary,
        ),
    )
}

@ThemePreviews
@Composable
private fun SVChipPreview() {
    PreviewContainer {
        Row(horizontalArrangement = Arrangement.spacedBy(SecureVisionDimens.spacingSmall)) {
            SVChip(label = "All", selected = true, onClick = {})
            SVChip(label = "Weapon", selected = false, onClick = {})
            SVChip(label = "Motion", selected = false, onClick = {})
        }
    }
}
