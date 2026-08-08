package com.securevision.core.model

/**
 * How urgent an alert is.
 *
 * Declared in ascending order of urgency, so the natural [Comparable] ordering of
 * the enum is meaningful and [isAtLeast] can be used for threshold filtering.
 */
enum class Severity {

    /** Routine, logged only. */
    LOW,

    /** Worth surfacing in the alerts list. */
    MEDIUM,

    /** Notified immediately. */
    HIGH,

    /** Notified immediately with the critical alarm — weapons and watchlist hits. */
    CRITICAL;

    /** Returns `true` when this severity is [other] or more urgent. */
    fun isAtLeast(other: Severity): Boolean = ordinal >= other.ordinal
}
