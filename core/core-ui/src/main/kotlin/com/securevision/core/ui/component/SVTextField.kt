package com.securevision.core.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import com.securevision.core.ui.R
import com.securevision.core.ui.preview.PreviewContainer
import com.securevision.core.ui.preview.ThemePreviews
import com.securevision.core.ui.theme.SecureVisionDimens

/**
 * The standard SecureVision text input.
 *
 * Password masking is handled here, including the reveal toggle, so no screen
 * has to reimplement it. The reveal state is local to the field on purpose: it
 * is transient view state, not something a ViewModel should carry.
 *
 * @param value Current text.
 * @param onValueChange Invoked on every edit.
 * @param label Field label.
 * @param modifier Modifier applied to the field.
 * @param isPassword Whether the text is masked and offers a reveal toggle.
 * @param error Validation message to show beneath the field; `null` when valid.
 * @param enabled Whether the field accepts input.
 * @param singleLine Whether the field is restricted to one line.
 * @param keyboardType Soft-keyboard type to request.
 * @param imeAction Action key shown on the soft keyboard.
 */
@Composable
fun SVTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    error: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = if (isPassword) KeyboardType.Password else KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
) {
    var passwordRevealed by remember { mutableStateOf(false) }
    val isError = error != null

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            label = { Text(text = label, style = MaterialTheme.typography.bodyMedium) },
            singleLine = singleLine,
            isError = isError,
            shape = MaterialTheme.shapes.medium,
            textStyle = MaterialTheme.typography.bodyLarge,
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = imeAction,
            ),
            visualTransformation = when {
                !isPassword || passwordRevealed -> VisualTransformation.None
                else -> PasswordVisualTransformation()
            },
            trailingIcon = if (isPassword) {
                {
                    PasswordRevealToggle(
                        revealed = passwordRevealed,
                        onToggle = { passwordRevealed = !passwordRevealed },
                    )
                }
            } else {
                null
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                cursorColor = MaterialTheme.colorScheme.primary,
            ),
        )

        if (error != null) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(
                    start = SecureVisionDimens.spacingMedium,
                    top = SecureVisionDimens.spacingExtraSmall,
                ),
            )
        }
    }
}

@Composable
private fun PasswordRevealToggle(revealed: Boolean, onToggle: () -> Unit) {
    val descriptionRes = if (revealed) {
        R.string.sv_content_description_hide_password
    } else {
        R.string.sv_content_description_show_password
    }

    IconButton(onClick = onToggle) {
        Icon(
            imageVector = if (revealed) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
            contentDescription = stringResource(descriptionRes),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@ThemePreviews
@Composable
private fun SVTextFieldPreview() {
    PreviewContainer {
        SVTextField(value = "hameed", onValueChange = {}, label = "Username")
        SVTextField(
            value = "hunter2",
            onValueChange = {},
            label = "Password",
            isPassword = true,
        )
        SVTextField(
            value = "42101",
            onValueChange = {},
            label = "CNIC",
            error = "CNIC must be 13 digits",
        )
    }
}
