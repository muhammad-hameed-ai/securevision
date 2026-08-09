package com.securevision.feature.auth.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.PersonOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import com.securevision.core.common.extension.toFormattedDate
import com.securevision.core.model.UserAccount
import com.securevision.core.ui.component.SVCard
import com.securevision.core.ui.component.SVEmptyState
import com.securevision.core.ui.preview.PreviewContainer
import com.securevision.core.ui.preview.ThemePreviews
import com.securevision.core.ui.theme.SecureVisionDimens
import com.securevision.feature.auth.R

/**
 * The signed-in operator's account details.
 *
 * @param uiState Current account state.
 * @param onLogout Invoked when the operator signs out.
 * @param modifier Modifier applied to the screen.
 */
@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        ProfileUiState.Loading -> Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }

        ProfileUiState.SignedOut -> SVEmptyState(
            icon = Icons.Outlined.PersonOff,
            title = stringResource(R.string.auth_profile_empty_title),
            subtitle = stringResource(R.string.auth_profile_empty_subtitle),
            modifier = modifier.fillMaxSize(),
        )

        is ProfileUiState.Content -> AccountDetails(
            account = uiState.account,
            onLogout = onLogout,
            modifier = modifier,
        )
    }
}

@Composable
private fun AccountDetails(
    account: UserAccount,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var cnicRevealed by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(SecureVisionDimens.spacingMedium),
        verticalArrangement = Arrangement.spacedBy(SecureVisionDimens.spacingMedium),
    ) {
        SVCard {
            DetailRow(
                label = stringResource(R.string.auth_profile_full_name),
                value = account.fullName,
            )
            DetailRow(
                label = stringResource(R.string.auth_profile_username),
                value = account.username,
                monospace = true,
            )

            // Masked by default. It is the operator's own identity number, but it
            // should not be readable by anyone glancing at the screen.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                DetailRow(
                    label = stringResource(R.string.auth_profile_cnic),
                    value = if (cnicRevealed) account.cnic.formatCnic() else account.cnic.maskCnic(),
                    monospace = true,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { cnicRevealed = !cnicRevealed }) {
                    Icon(
                        imageVector = if (cnicRevealed) {
                            Icons.Filled.VisibilityOff
                        } else {
                            Icons.Filled.Visibility
                        },
                        contentDescription = stringResource(
                            if (cnicRevealed) {
                                com.securevision.core.ui.R.string.sv_content_description_hide_password
                            } else {
                                com.securevision.core.ui.R.string.sv_content_description_show_password
                            },
                        ),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            DetailRow(
                label = stringResource(R.string.auth_profile_member_since),
                value = account.createdAt.toFormattedDate(),
            )
        }

        Text(
            text = stringResource(R.string.auth_profile_storage_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(R.string.auth_profile_logout),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    monospace: Boolean = false,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(SecureVisionDimens.spacingExtraSmall),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = if (monospace) {
                MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace)
            } else {
                MaterialTheme.typography.bodyLarge
            },
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** Renders thirteen bare digits in the familiar `#####-#######-#` grouping. */
private fun String.formatCnic(): String =
    if (length != CNIC_LENGTH) {
        this
    } else {
        "${substring(0, 5)}-${substring(5, 12)}-${substring(12)}"
    }

/** Keeps the shape of a CNIC while revealing none of it. */
private fun String.maskCnic(): String =
    if (length != CNIC_LENGTH) "*".repeat(length) else "*****-*******-*"

private const val CNIC_LENGTH = 13

@ThemePreviews
@Composable
private fun ProfileScreenPreview() {
    PreviewContainer {
        ProfileScreen(
            uiState = ProfileUiState.Content(
                account = UserAccount(
                    uid = "uid-1",
                    username = "hameed",
                    fullName = "Muhammad Hameed",
                    cnic = "4210112345671",
                    createdAt = 1_754_000_000_000L,
                ),
            ),
            onLogout = {},
        )
    }
}
