package com.securevision

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.securevision.ui.SecureVisionAppShell
import com.securevision.core.ui.theme.SecureVisionTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * The single Activity hosting every SecureVision screen.
 *
 * Holds no logic of its own: it collects [MainViewModel]'s state and hands it to
 * the shell, which owns navigation.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val darkMode by viewModel.darkMode.collectAsStateWithLifecycle()

            SecureVisionTheme(darkTheme = darkMode) {
                SecureVisionAppShell(
                    uiState = uiState,
                    onLogout = viewModel::logout,
                )
            }
        }
    }
}
