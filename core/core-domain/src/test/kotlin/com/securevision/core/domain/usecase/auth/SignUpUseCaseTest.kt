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

/** Pins the sign-up credential rules, including CNIC normalisation. */
class SignUpUseCaseTest {

    private val authRepository = mockk<AuthRepository>(relaxed = true)

    @Test
    fun `creates the account when every rule passes`() = runTest {
        coEvery { authRepository.signUp(any(), any(), any(), any()) } returns ACCOUNT

        val result = useCase()(validParams())

        assertEquals(Result.Success(ACCOUNT), result)
    }

    @Test
    fun `normalises a separated CNIC to bare digits before storing it`() = runTest {
        coEvery { authRepository.signUp(any(), any(), any(), any()) } returns ACCOUNT

        useCase()(validParams(cnic = "42101-1234567-1"))

        coVerify(exactly = 1) {
            authRepository.signUp(USERNAME, FULL_NAME, PASSWORD, "4210112345671")
        }
    }

    @Test
    fun `rejects a CNIC with the wrong number of digits`() = runTest {
        val result = useCase()(validParams(cnic = "42101-12345-1"))

        assertReason(result, AuthValidationException.Reason.INVALID_CNIC)
        coVerify(exactly = 0) { authRepository.signUp(any(), any(), any(), any()) }
    }

    @Test
    fun `rejects a username shorter than the minimum`() = runTest {
        val result = useCase()(validParams(username = "ab"))

        assertReason(result, AuthValidationException.Reason.USERNAME_TOO_SHORT)
    }

    @Test
    fun `rejects a blank full name`() = runTest {
        val result = useCase()(validParams(fullName = "   "))

        assertReason(result, AuthValidationException.Reason.BLANK_FULL_NAME)
    }

    @Test
    fun `rejects a password shorter than the minimum`() = runTest {
        val result = useCase()(validParams(password = "12345"))

        assertReason(result, AuthValidationException.Reason.PASSWORD_TOO_SHORT)
    }

    @Test
    fun `reports a blank password as blank rather than too short`() = runTest {
        val result = useCase()(validParams(password = ""))

        assertReason(result, AuthValidationException.Reason.BLANK_PASSWORD)
    }

    /**
     * Declared on [TestScope] so the use case's dispatcher shares `runTest`'s
     * scheduler — a dispatcher with its own scheduler would never be advanced and
     * every test here would hang.
     */
    private fun TestScope.useCase() = SignUpUseCase(
        authRepository = authRepository,
        dispatcherProvider = TestDispatcherProvider(StandardTestDispatcher(testScheduler)),
    )

    private fun validParams(
        username: String = USERNAME,
        fullName: String = FULL_NAME,
        password: String = PASSWORD,
        cnic: String = "4210112345671",
    ) = SignUpUseCase.Params(
        username = username,
        fullName = fullName,
        password = password,
        cnic = cnic,
    )

    private fun assertReason(result: Result<UserAccount>, expected: AuthValidationException.Reason) {
        assertTrue(result is Result.Error)
        val throwable = (result as Result.Error).throwable
        assertTrue(throwable is AuthValidationException)
        assertEquals(expected, (throwable as AuthValidationException).reason)
    }

    private companion object {
        const val USERNAME = "hameed"
        const val FULL_NAME = "Muhammad Hameed"
        const val PASSWORD = "correct-horse"

        val ACCOUNT = UserAccount(
            uid = "uid-1",
            username = USERNAME,
            fullName = FULL_NAME,
            cnic = "4210112345671",
            createdAt = 1_700_000_000_000L,
        )
    }
}
