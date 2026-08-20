package com.securevision.feature.alerts

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securevision.core.domain.repository.AlertRepository
import com.securevision.core.model.AlertRecord
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Loads one alert for the detail view.
 *
 * Reads the id from [SavedStateHandle] rather than taking it as a parameter, so
 * the screen survives process death without the caller re-supplying it.
 */
@HiltViewModel
class AlertDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: AlertRepository,
) : ViewModel() {

    private val _alert = MutableStateFlow<AlertRecord?>(null)

    /** The alert being shown, or `null` while loading or if it has been dismissed. */
    val alert: StateFlow<AlertRecord?> = _alert.asStateFlow()

    private val _isLoading = MutableStateFlow(true)

    /** Whether the lookup is still running, so "missing" is not shown prematurely. */
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        val id: String? = savedStateHandle[AlertsRoutes.ARG_ALERT_ID]

        viewModelScope.launch {
            _alert.value = id?.let { repository.getById(it) }
            _isLoading.value = false
        }
    }
}
