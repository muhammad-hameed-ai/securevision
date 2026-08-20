package com.securevision.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securevision.core.common.result.getOrDefault
import com.securevision.core.domain.usecase.dashboard.GetUnreadAlertCountUseCase
import com.securevision.core.domain.usecase.invoke
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * State the app shell needs regardless of which screen is showing.
 *
 * Only the unread count so far. It lives here rather than on the Alerts screen
 * because the drawer badge has to be current wherever the user is — an operator
 * on the live camera needs to see that three alerts have piled up without
 * navigating away to find out.
 */
@HiltViewModel
class AppShellViewModel @Inject constructor(
    getUnreadAlertCount: GetUnreadAlertCountUseCase,
) : ViewModel() {

    /** Live count of unacknowledged alerts. */
    val unreadAlerts: StateFlow<Int> = getUnreadAlertCount()
        .map { result -> result.getOrDefault(0) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MILLIS),
            initialValue = 0,
        )

    private companion object {
        const val SUBSCRIPTION_TIMEOUT_MILLIS = 5_000L
    }
}
