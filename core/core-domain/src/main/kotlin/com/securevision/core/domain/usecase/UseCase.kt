package com.securevision.core.domain.usecase

import com.securevision.core.common.result.Result
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

/**
 * Base class for a one-shot unit of business logic.
 *
 * Subclasses implement [execute] and get three guarantees for free:
 *
 * 1. the work runs on the injected [dispatcher], never the caller's thread;
 * 2. a thrown exception becomes [Result.Error] instead of escaping into the UI;
 * 3. cancellation still propagates, so structured concurrency is preserved.
 *
 * Invoke it like a function: `loginUseCase(LoginUseCase.Params(name, password))`.
 *
 * Project convention for [P]: take the domain model directly when the input *is*
 * a domain model, a nested `Params` data class when the input is several values
 * or a bare primitive that needs a name, and [Unit] when there is no input.
 *
 * @param P parameter type; use [Unit] for a use case that takes no input.
 * @param R type of the produced value.
 * @property dispatcher Dispatcher the work is confined to.
 */
abstract class UseCase<in P, R>(private val dispatcher: CoroutineDispatcher) {

    /**
     * Runs the use case.
     *
     * @param parameters Input for this invocation.
     * @return [Result.Success] with the produced value, or [Result.Error].
     */
    suspend operator fun invoke(parameters: P): Result<R> = try {
        withContext(dispatcher) {
            Result.Success(execute(parameters))
        }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (throwable: Throwable) {
        Result.Error(throwable, throwable.message)
    }

    /**
     * The actual work. Called on [dispatcher]; may throw freely.
     *
     * @param parameters Input for this invocation.
     */
    protected abstract suspend fun execute(parameters: P): R
}

/** Convenience invocation for a use case that takes no parameters. */
suspend operator fun <R> UseCase<Unit, R>.invoke(): Result<R> = invoke(Unit)
