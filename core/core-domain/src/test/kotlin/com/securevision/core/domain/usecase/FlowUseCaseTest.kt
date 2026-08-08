package com.securevision.core.domain.usecase

import app.cash.turbine.test
import com.securevision.core.common.result.Result
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Covers the streaming contract of [FlowUseCase]. */
class FlowUseCaseTest {

    @Test
    fun `opens with Loading then emits each value as Success`() = runTest {
        val useCase = CountingFlowUseCase(StandardTestDispatcher(testScheduler))

        useCase(3).test {
            assertEquals(Result.Loading, awaitItem())
            assertEquals(Result.Success(1), awaitItem())
            assertEquals(Result.Success(2), awaitItem())
            assertEquals(Result.Success(3), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `converts an upstream failure into a terminal Error`() = runTest {
        val useCase = FailingFlowUseCase(StandardTestDispatcher(testScheduler))

        useCase(Unit).test {
            assertEquals(Result.Loading, awaitItem())
            val error = awaitItem()
            assertTrue(error is Result.Error)
            assertEquals("stream died", (error as Result.Error).message)
            awaitComplete()
        }
    }

    @Test
    fun `supports parameterless invocation`() = runTest {
        val useCase = SingleValueFlowUseCase(StandardTestDispatcher(testScheduler))

        useCase().test {
            assertEquals(Result.Loading, awaitItem())
            assertEquals(Result.Success("only"), awaitItem())
            awaitComplete()
        }
    }

    private class CountingFlowUseCase(dispatcher: CoroutineDispatcher) :
        FlowUseCase<Int, Int>(dispatcher) {
        override fun execute(parameters: Int): Flow<Int> = flow {
            for (value in 1..parameters) emit(value)
        }
    }

    private class FailingFlowUseCase(dispatcher: CoroutineDispatcher) :
        FlowUseCase<Unit, Int>(dispatcher) {
        override fun execute(parameters: Unit): Flow<Int> = flow {
            throw IllegalStateException("stream died")
        }
    }

    private class SingleValueFlowUseCase(dispatcher: CoroutineDispatcher) :
        FlowUseCase<Unit, String>(dispatcher) {
        override fun execute(parameters: Unit): Flow<String> = flowOf("only")
    }
}
