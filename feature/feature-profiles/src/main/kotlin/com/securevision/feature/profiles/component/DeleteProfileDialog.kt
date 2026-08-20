package com.securevision.feature.profiles.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.securevision.feature.profiles.R

/**
 * Confirms removing an enrolled person.
 *
 * The copy names what is actually lost rather than asking a generic "are you
 * sure?". Deleting here destroys a face embedding that exists in one place on one
 * device with no backup — the only way to undo it is to bring the person back in
 * front of the camera, and the dialog says so.
 *
 * @param profileName Whose profile is being removed.
 * @param onConfirm Proceeds with deletion.
 * @param onDismiss Cancels.
 */
@Composable
fun DeleteProfileDialog(
    profileName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.profiles_delete_title, profileName)) },
        text = { Text(stringResource(R.string.profiles_delete_body)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.profiles_delete_confirm),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}
