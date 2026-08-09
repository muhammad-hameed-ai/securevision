package com.securevision.core.domain.usecase.auth

import com.securevision.core.common.result.Result
import com.securevision.core.domain.repository.AuthRepository
import com.securevision.core.domain.usecase.TestDispatcherProvider
import com.securevision.core.model.UserAccount
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Verifies that login validation happens in the domain layer, before any I/O. */
class LoginUseCaseTest {

    private val authRepository = mockk<AuthRepository>(relaxed = true)

    @Test
    fun `returns the account on a successful login`() = runTest {
        coEvery { authRepository.login(USERNAME, PASSWORD) } returns ACCOUNT

        val result = useCase()(LoginUseCase.Params(USERNAME, PASSWORD))

        assertEquals(Result.Success(ACCOUNT), result)
    }

    @Test
    fun `trims surrounding whitespace from the username`() = runTest {
        coEvery { authRepository.login(USERNAME, PASSWORD) } returns ACCOUNT

        useCase()(LoginUseCase.Params("  $USERNAME  ", PASSWORD))

        coVerify(exactly = 1) { authRepository.login(USERNAME, PASSWORD) }
    }

    @Test
    fun `rejects a blank username without calling the repository`() = runTest {
        val result = useCase()(LoginUseCase.Params("   ", PASSWORD))

        assertReason(result, AuthValidationException.Reason.BLANK_USERNAME)
        coVerify(exactly = 0) { authRepository.login(any(), any()) }
    }

    @Test
    fun `rejects a blank password without calling the repository`() = runTest {
        val result = useCase()(LoginUseCase.Params(USERNAME, ""))

        assertReason(result, AuthValidationException.Reason.BLANK_PASSWORD)
        coVerify(exactly = 0) { authRepository.login(any(), any()) }
    }

    @Test
    fun `does not apply the length rule to an existing password`() = runTest {
        // Six characters — below today's minimum, but an account created under the
        // Phase 1 rule must still be able to sign in rather than be locked out.
        coEvery { authRepository.login(USERNAME, "short1") } returns ACCOUNT

        val result = useCase()(LoginUseCase.Params(USERNAME, "short1"))

        assertTrue(result is Result.Success)
        coVerify(exactly = 1) { authRepository.login(USERNAME, "short1") }
    }

    @Test
    fun `surfaces a rejected credential as an Error carrying its reason`() = runTest {
        coEvery { authRepository.login(any(), any()) } throws
            AuthValidationException(AuthValidationException.Reason.INVALID_CREDENTIALS)

        assertReason(
            useCase()(LoginUseCase.Params(USERNAME, PASSWORD)),
            AuthValidationException.Reason.INVALID_CREDENTIALS,
        )
    }

    /**
     * Declared on [TestScope] so the use case's dispatcher shares `runTest`'s
     * scheduler — a dispatcher with its own scheduler would never be advanced and
     * every test here would hang.
     */
    private fun TestScope.useCase() = LoginUseCase(
        authRepository = authRepository,
        dispatcherProvider = TestDispatcherProvider(StandardTestDispatcher(testScheduler)),
    )

    private fun assertReason(result: Result<UserAccount>, expected: AuthValidationException.Reason) {
        assertTrue(result is Result.Error)
        val throwable = (result as Result.Error).throwable
        assertTrue(throwable is AuthValidationException)
        assertEquals(expected, (throwable as AuthValidationException).reason)
    }

    private companion object {
        const val USERNAME = "hameed"
        const val PASSWORD = "correct-horse"

        val ACCOUNT = UserAccount(
            uid = "uid-1",
            username = USERNAME,
            fullName = "Muhammad Hameed",
            cnic = "4210112345671",
            createdAt = 1_754_000_000_000L,
        )
    }
}
