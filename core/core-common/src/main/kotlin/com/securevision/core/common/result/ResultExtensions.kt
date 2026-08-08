package com.securevision.core.common.result

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

/** `true` when this is [Result.Success]. */
val Result<*>.isSuccess: Boolean get() = this is Result.Success

/** `true` when this is [Result.Error]. */
val Result<*>.isError: Boolean get() = this is Result.Error

/** `true` when this is [Result.Loading]. */
val Result<*>.isLoading: Boolean get() = this is Result.Loading

/** The success value, or `null` for [Result.Error] and [Result.Loading]. */
fun <T> Result<T>.getOrNull(): T? = (this as? Result.Success)?.data

/** The success value, or [fallback] for [Result.Error] and [Result.Loading]. */
fun <T> Result<T>.getOrDefault(fallback: T): T = getOrNull() ?: fallback

/**
 * Transforms a successful value, leaving [Result.Error] and [Result.Loading]
 * untouched.
 *
 * @param transform Mapping applied to the success value.
 */
inline fun <T, R> Result<T>.map(transform: (T) -> R): Result<R> = when (this) {
    is Result.Success -> Result.Success(transform(data))
    is Result.Error -> this
    Result.Loading -> Result.Loading
}

/**
 * Like [map], but converts a throwing [transform] into [Result.Error] instead of
 * letting the exception propagate.
 */
inline fun <T, R> Result<T>.mapCatching(transform: (T) -> R): Result<R> = when (this) {
    is Result.Success -> runCatching { Result.Success(transform(data)) }
        .getOrElse { Result.Error(it) }
    is Result.Error -> this
    Result.Loading -> Result.Loading
}

/**
 * Collapses every case into a single value — the exhaustive alternative to a
 * chain of `if` checks.
 *
 * @param onSuccess Applied to the success value.
 * @param onError Applied to the cause and optional message.
 * @param onLoading Supplies the value used while in flight.
 */
inline fun <T, R> Result<T>.fold(
    onSuccess: (T) -> R,
    onError: (Throwable, String?) -> R,
    onLoading: () -> R,
): R = when (this) {
    is Result.Success -> onSuccess(data)
    is Result.Error -> onError(throwable, message)
    Result.Loading -> onLoading()
}

/**
 * Runs [action] when this is [Result.Success] and returns the receiver, so calls
 * can be chained.
 */
inline fun <T> Result<T>.onSuccess(action: (T) -> Unit): Result<T> = apply {
    if (this is Result.Success) action(data)
}

/**
 * Runs [action] when this is [Result.Error] and returns the receiver, so calls
 * can be chained.
 */
inline fun <T> Result<T>.onError(action: (Throwable, String?) -> Unit): Result<T> = apply {
    if (this is Result.Error) action(throwable, message)
}

/**
 * Runs [action] when this is [Result.Loading] and returns the receiver, so calls
 * can be chained.
 */
inline fun <T> Result<T>.onLoading(action: () -> Unit): Result<T> = apply {
    if (this is Result.Loading) action()
}

/**
 * Executes [block], wrapping its outcome in a [Result].
 *
 * Cancellation is deliberately not caught: a [kotlin.coroutines.cancellation.CancellationException]
 * must keep propagating so that structured concurrency still works.
 *
 * @param block The work to run.
 */
inline fun <T> resultOf(block: () -> T): Result<T> = try {
    Result.Success(block())
} catch (cancellation: kotlin.coroutines.cancellation.CancellationException) {
    throw cancellation
} catch (throwable: Throwable) {
    Result.Error(throwable, throwable.message)
}

/**
 * Lifts a plain stream into a [Result] stream: emits [Result.Loading] first, then
 * [Result.Success] per element, and converts an upstream failure into a terminal
 * [Result.Error] rather than a crash.
 */
fun <T> Flow<T>.asResult(): Flow<Result<T>> = this
    .map<T, Result<T>> { Result.Success(it) }
    .onStart { emit(Result.Loading) }
    .catch { emit(Result.Error(it, it.message)) }
