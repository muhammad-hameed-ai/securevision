package com.securevision.core.common.extension

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

/**
 * Shares this flow as a [StateFlow] that stays active for [stopTimeoutMillis]
 * after the last collector leaves.
 *
 * The timeout is what makes a configuration change cheap: the ViewModel's
 * upstream is not torn down and restarted when the Activity is recreated, but it
 * is stopped when the app genuinely goes to the background.
 *
 * @param scope Scope that owns the sharing, normally `viewModelScope`.
 * @param initialValue Value exposed before the upstream emits.
 * @param stopTimeoutMillis Grace period before the upstream is cancelled.
 */
fun <T> Flow<T>.stateInWhileSubscribed(
    scope: CoroutineScope,
    initialValue: T,
    stopTimeoutMillis: Long = DEFAULT_STOP_TIMEOUT_MILLIS,
): StateFlow<T> = stateIn(
    scope = scope,
    started = SharingStarted.WhileSubscribed(stopTimeoutMillis),
    initialValue = initialValue,
)

/**
 * Emits the first value, then drops any value arriving within [windowMillis] of
 * the last emission.
 *
 * Rate-limits repeated detections of the same event — an unknown face present for
 * ten seconds must not queue up three hundred notifications.
 *
 * @param windowMillis Minimum gap between emissions.
 */
fun <T> Flow<T>.throttleFirst(windowMillis: Long): Flow<T> = flow {
    var lastEmissionAt = 0L
    collect { value ->
        val now = System.currentTimeMillis()
        if (now - lastEmissionAt >= windowMillis) {
            lastEmissionAt = now
            emit(value)
        }
    }
}

/** Five seconds — long enough to survive a rotation, short enough to release the camera. */
private const val DEFAULT_STOP_TIMEOUT_MILLIS = 5_000L
