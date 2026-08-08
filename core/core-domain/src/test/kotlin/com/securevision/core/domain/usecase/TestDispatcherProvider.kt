package com.securevision.core.domain.usecase

import com.securevision.core.common.dispatcher.DispatcherProvider
import kotlinx.coroutines.CoroutineDispatcher

/**
 * Routes every dispatcher to one test dispatcher so use case work runs on the
 * test scheduler and `runTest` stays deterministic.
 *
 * @property testDispatcher Dispatcher returned for [io], [default] and [main].
 */
class TestDispatcherProvider(
    private val testDispatcher: CoroutineDispatcher,
) : DispatcherProvider {

    override val io: CoroutineDispatcher = testDispatcher

    override val default: CoroutineDispatcher = testDispatcher

    override val main: CoroutineDispatcher = testDispatcher
}
