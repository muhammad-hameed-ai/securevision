package com.securevision.core.domain.usecase

import com.securevision.core.common.result.Result
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the three guarantees [UseCase] makes to every subclass: dispatcher
 * confinement, failure wrapping, and cancellation transparency.
 */
class UseCaseTest {

    @Test
    fun `wraps a returned value in Success`() = runTest {
        val useCase = DoublingUseCase(StandardTestDispatcher(testScheduler))

        assertEquals(Result.Success(84), useCase(42))
    }

    @Test
    fun `wraps a thrown exception in Error carrying its message`() = runTest {
        val useCase = FailingUseCase(StandardTestDispatcher(testScheduler))

        val result = useCase(Unit)

        assertTrue(result is Result.Error)
        result as Result.Error
        assertEquals("deliberate failure", result.message)
        assertTrue(result.throwable is IllegalStateException)
    }

    @Test(expected = CancellationException::class)
    fun `lets cancellation propagate instead of swallowing it`() = runTest {
        val useCase = CancellingUseCase(StandardTestDispatcher(testScheduler))

        useCase(Unit)
    }

    @Test
    fun `confines execution to the injected dispatcher`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val useCase = DispatcherReportingUseCase(dispatcher)

        val result = useCase(Unit)

        assertEquals(Result.Success(dispatcher), result)
        assertSame(dispatcher, (result as Result.Success).data)
    }

    @Test
    fun `supports parameterless invocation`() = runTest {
        val useCase = GreetingUseCase(StandardTestDispatcher(testScheduler))

        assertEquals(Result.Success("hello"), useCase())
    }

    private class DoublingUseCase(dispatcher: CoroutineDispatcher) :
        UseCase<Int, Int>(dispatcher) {
        override suspend fun execute(parameters: Int): Int = parameters * 2
    }

    private class FailingUseCase(dispatcher: CoroutineDispatcher) :
        UseCase<Unit, Nothing>(dispatcher) {
        override suspend fun execute(parameters: Unit): Nothing = error("deliberate failure")
    }

    private class CancellingUseCase(dispatcher: CoroutineDispatcher) :
        UseCase<Unit, Nothing>(dispatcher) {
        override suspend fun execute(parameters: Unit): Nothing =
            throw CancellationException("cancelled")
    }

    private class DispatcherReportingUseCase(dispatcher: CoroutineDispatcher) :
        UseCase<Unit, CoroutineDispatcher?>(dispatcher) {
        override suspend fun execute(parameters: Unit): CoroutineDispatcher? =
            currentCoroutineContext()[ContinuationInterceptor] as? CoroutineDispatcher
    }

    private class GreetingUseCase(dispatcher: CoroutineDispatcher) :
        UseCase<Unit, String>(dispatcher) {
        override suspend fun execute(parameters: Unit): String = "hello"
    }
}
