package com.securevision.core.ui.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.securevision.core.ui.theme.SecureVisionDimens
import com.securevision.core.ui.theme.SecureVisionTheme

/**
 * Wraps preview content in the real theme on the real background colour.
 *
 * Previewing a component against the tooling's default white would hide exactly
 * the contrast problems previews exist to catch.
 *
 * @param content The component being previewed.
 */
@Composable
fun PreviewContainer(content: @Composable () -> Unit) {
    SecureVisionTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(SecureVisionDimens.spacingMedium),
                verticalArrangement = Arrangement.spacedBy(SecureVisionDimens.spacingSmall),
            ) {
                content()
            }
        }
    }
}
