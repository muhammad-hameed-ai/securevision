package com.securevision.core.common.result

/**
 * Outcome of an operation that can be in flight, succeed, or fail.
 *
 * Used as the return type of every use case and as the payload of every
 * repository stream, so that a failure is a value the caller must handle rather
 * than an exception that escapes into the UI layer.
 *
 * Deliberately distinct from [kotlin.Result]: this type models an in-flight
 * [Loading] state, which is what lets a screen render a spinner from the same
 * stream that later carries the data.
 *
 * @param T type of the value carried by [Success].
 */
sealed interface Result<out T> {

    /**
     * The operation completed and produced [data].
     *
     * @property data The produced value.
     */
    data class Success<out T>(val data: T) : Result<T>

    /**
     * The operation failed.
     *
     * @property throwable The cause, retained for logging and diagnostics.
     * @property message Optional human-readable explanation. When `null`, the
     *   presentation layer supplies its own localised fallback — never show a raw
     *   exception message to a user.
     */
    data class Error(
        val throwable: Throwable,
        val message: String? = null,
    ) : Result<Nothing>

    /** The operation has started and has not yet produced a value. */
    data object Loading : Result<Nothing>
}
