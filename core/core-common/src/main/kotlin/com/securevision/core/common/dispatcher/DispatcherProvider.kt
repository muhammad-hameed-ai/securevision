package com.securevision.core.common.dispatcher

import kotlinx.coroutines.CoroutineDispatcher

/**
 * Supplies the coroutine dispatchers used across the app.
 *
 * Injected rather than referenced statically so that tests can substitute a
 * single deterministic test dispatcher, and so no class ever hard-codes
 * `Dispatchers.IO`.
 */
interface DispatcherProvider {

    /** For blocking I/O: database, file system, network. */
    val io: CoroutineDispatcher

    /** For CPU-bound work: image pre-processing, embedding maths, sorting. */
    val default: CoroutineDispatcher

    /** For UI-thread work. Never used for anything that can block. */
    val main: CoroutineDispatcher
}
