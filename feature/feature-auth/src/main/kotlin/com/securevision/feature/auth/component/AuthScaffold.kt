package com.securevision.feature.auth.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.securevision.core.ui.preview.PreviewContainer
import com.securevision.core.ui.preview.ThemePreviews
import com.securevision.core.ui.theme.SecureVisionDimens
import com.securevision.feature.auth.R

/**
 * Shared framing for every authentication screen.
 *
 * Gives Login, Sign-up and recovery one identical layout so moving between them
 * does not shift the wordmark or the field column. Scrolls and applies
 * `imePadding` because the sign-up form is taller than a phone screen once the
 * keyboard is open.
 *
 * @param title Screen heading.
 * @param subtitle One line explaining what the screen is for.
 * @param modifier Modifier applied to the surface.
 * @param content Form fields and actions, laid out in a column.
 */
@Composable
fun AuthScaffold(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(
                    horizontal = SecureVisionDimens.spacingLarge,
                    vertical = SecureVisionDimens.spacingExtraLarge,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = MAX_FORM_WIDTH),
                verticalArrangement = Arrangement.spacedBy(SecureVisionDimens.spacingMedium),
            ) {
                Text(
                    text = stringResource(R.string.auth_wordmark),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(
                        SecureVisionDimens.spacingExtraSmall,
                    ),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                content()
            }
        }
    }
}

/** Keeps the form a comfortable reading width on a tablet. */
private val MAX_FORM_WIDTH = 420.dp

@ThemePreviews
@Composable
private fun AuthScaffoldPreview() {
    PreviewContainer {
        AuthScaffold(
            title = "Welcome back",
            subtitle = "Sign in to your operator account.",
        ) {
            Text("form fields go here")
        }
    }
}
