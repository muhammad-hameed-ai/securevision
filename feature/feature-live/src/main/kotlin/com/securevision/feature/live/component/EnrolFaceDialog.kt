package com.securevision.feature.live.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.securevision.core.ui.component.SVTextField
import com.securevision.core.ui.preview.PreviewContainer
import com.securevision.core.ui.preview.ThemePreviews
import com.securevision.core.ui.theme.SecureVisionDimens
import com.securevision.feature.live.R

/**
 * Collects a name and age for the face currently in frame.
 *
 * **Temporary, Phase 4.** The polished enrolment experience belongs to the
 * Profiles screen in a later phase; this exists so the recognition path can be
 * tested at all. What must be reused rather than reimplemented is the embedding
 * path behind it — a second one would drift from the one recognition uses.
 *
 * @param isEnrolling Whether a capture is in flight.
 * @param onConfirm Invoked with the entered name and age.
 * @param onDismiss Invoked when the dialog is cancelled.
 */
@Composable
fun EnrolFaceDialog(
    isEnrolling: Boolean,
    onConfirm: (name: String, age: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }

    val parsedAge = age.toIntOrNull()
    val canConfirm = name.isNotBlank() && parsedAge != null && !isEnrolling

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.live_enrol_title)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(SecureVisionDimens.spacingMediumSmall),
            ) {
                Text(
                    text = stringResource(R.string.live_enrol_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                SVTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = stringResource(R.string.live_enrol_name),
                    enabled = !isEnrolling,
                )

                SVTextField(
                    value = age,
                    onValueChange = { input -> age = input.filter(Char::isDigit) },
                    label = stringResource(R.string.live_enrol_age),
                    enabled = !isEnrolling,
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { parsedAge?.let { onConfirm(name, it) } },
                enabled = canConfirm,
            ) {
                Text(
                    text = stringResource(
                        if (isEnrolling) R.string.live_enrol_saving else R.string.live_enrol_confirm,
                    ),
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isEnrolling) {
                Text(stringResource(R.string.live_enrol_cancel))
            }
        },
        modifier = Modifier,
    )
}

@ThemePreviews
@Composable
private fun EnrolFaceDialogPreview() {
    PreviewContainer {
        EnrolFaceDialog(isEnrolling = false, onConfirm = { _, _ -> }, onDismiss = {})
    }
}
