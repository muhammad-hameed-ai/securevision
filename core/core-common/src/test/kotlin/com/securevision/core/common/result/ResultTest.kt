package com.securevision.core.common.result

import app.cash.turbine.test
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** Covers the [Result] combinators that every use case and ViewModel depends on. */
class ResultTest {

    @Test
    fun `map transforms a success value`() {
        val mapped = Result.Success(21).map { it * 2 }

        assertEquals(Result.Success(42), mapped)
    }

    @Test
    fun `map leaves an error untouched`() {
        val cause = IllegalStateException("boom")

        val mapped = Result.Error(cause, "boom").map { "never" }

        assertEquals(Result.Error(cause, "boom"), mapped)
    }

    @Test
    fun `map leaves loading untouched`() {
        assertEquals(Result.Loading, Result.Loading.map { "never" })
    }

    @Test
    fun `mapCatching converts a thrown exception into an error`() {
        val mapped = Result.Success(0).mapCatching { 1 / it }

        assertTrue(mapped is Result.Error)
        assertTrue((mapped as Result.Error).throwable is ArithmeticException)
    }

    @Test
    fun `fold selects the branch matching the case`() {
        assertEquals("ok:7", Result.Success(7).describe())
        assertEquals("err:bad", Result.Error(RuntimeException(), "bad").describe())
        assertEquals("loading", Result.Loading.describe())
    }

    @Test
    fun `onSuccess runs only for success and returns the receiver`() {
        var observed: Int? = null
        val original: Result<Int> = Result.Success(5)

        val returned = original.onSuccess { observed = it }

        assertEquals(5, observed)
        assertSame(original, returned)
    }

    @Test
    fun `onError runs only for error`() {
        var observedMessage: String? = null

        Result.Success(5).onError { _, message -> observedMessage = message }
        assertNull(observedMessage)

        Result.Error(RuntimeException(), "failed").onError { _, message -> observedMessage = message }
        assertEquals("failed", observedMessage)
    }

    @Test
    fun `getOrNull and getOrDefault unwrap only a success`() {
        assertEquals(9, Result.Success(9).getOrNull())
        assertNull(Result.Error(RuntimeException()).getOrNull())
        assertNull(Result.Loading.getOrNull())

        assertEquals(9, Result.Success(9).getOrDefault(0))
        assertEquals(0, Result.Error(RuntimeException()).getOrDefault(0))
        assertEquals(0, Result.Loading.getOrDefault(0))
    }

    @Test
    fun `state predicates report the current case`() {
        assertTrue(Result.Success(Unit).isSuccess)
        assertFalse(Result.Success(Unit).isError)

        assertTrue(Result.Error(RuntimeException()).isError)
        assertTrue(Result.Loading.isLoading)
    }

    @Test
    fun `resultOf wraps a returned value`() {
        assertEquals(Result.Success("done"), resultOf { "done" })
    }

    @Test
    fun `resultOf captures a thrown exception`() {
        val result = resultOf { error("exploded") }

        assertTrue(result is Result.Error)
        assertEquals("exploded", (result as Result.Error).message)
    }

    @Test(expected = CancellationException::class)
    fun `resultOf rethrows cancellation so structured concurrency still works`() {
        resultOf { throw CancellationException("cancelled") }
    }

    @Test
    fun `asResult emits loading before each value`() = runTest {
        flowOf(1, 2).asResult().test {
            assertEquals(Result.Loading, awaitItem())
            assertEquals(Result.Success(1), awaitItem())
            assertEquals(Result.Success(2), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `asResult converts an upstream failure into a terminal error`() = runTest {
        val failing = flow<Int> { throw IllegalStateException("upstream died") }

        failing.asResult().test {
            assertEquals(Result.Loading, awaitItem())
            val error = awaitItem()
            assertTrue(error is Result.Error)
            assertEquals("upstream died", (error as Result.Error).message)
            awaitComplete()
        }
    }

    private fun Result<Int>.describe(): String = fold(
        onSuccess = { "ok:$it" },
        onError = { _, message -> "err:$message" },
        onLoading = { "loading" },
    )
}
