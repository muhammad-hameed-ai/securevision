package com.securevision.core.domain.usecase

import com.securevision.core.common.result.Result
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

/**
 * Base class for a use case that produces a continuous stream rather than a
 * single value.
 *
 * The streaming counterpart of [UseCase]: the source runs on the injected
 * [dispatcher], every element is wrapped in [Result.Success], the stream opens
 * with [Result.Loading], and an upstream failure becomes a terminal
 * [Result.Error] instead of crashing the collector.
 *
 * @param P parameter type; use [Unit] for a stream that takes no input.
 * @param R type of each produced element.
 * @property dispatcher Dispatcher the source is confined to.
 */
abstract class FlowUseCase<in P, R>(private val dispatcher: CoroutineDispatcher) {

    /**
     * Opens the stream.
     *
     * @param parameters Input for this stream.
     */
    operator fun invoke(parameters: P): Flow<Result<R>> = execute(parameters)
        .flowOn(dispatcher)
        .map<R, Result<R>> { Result.Success(it) }
        .onStart { emit(Result.Loading) }
        .catch { throwable -> emit(Result.Error(throwable, throwable.message)) }

    /**
     * The source stream. Collected on [dispatcher]; may throw freely.
     *
     * @param parameters Input for this stream.
     */
    protected abstract fun execute(parameters: P): Flow<R>
}

/** Convenience invocation for a streaming use case that takes no parameters. */
operator fun <R> FlowUseCase<Unit, R>.invoke(): Flow<Result<R>> = invoke(Unit)
