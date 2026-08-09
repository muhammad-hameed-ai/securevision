package com.securevision.core.domain.usecase.auth

import com.securevision.core.common.result.Result
import com.securevision.core.domain.repository.AuthRepository
import com.securevision.core.domain.usecase.TestDispatcherProvider
import com.securevision.core.model.AccountCreation
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
        coEvery { authRepository.signUp(any(), any(), any(), any()) } returns CREATION

        val result = useCase()(validParams())

        assertEquals(Result.Success(CREATION), result)
    }

    @Test
    fun `normalises a separated CNIC to bare digits before storing it`() = runTest {
        coEvery { authRepository.signUp(any(), any(), any(), any()) } returns CREATION

        useCase()(validParams(cnic = "42101-1234567-1"))

        coVerify(exactly = 1) {
            authRepository.signUp(USERNAME, FULL_NAME, "4210112345671", PASSWORD)
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
        assertReason(
            useCase()(validParams(username = "ab")),
            AuthValidationException.Reason.USERNAME_TOO_SHORT,
        )
    }

    @Test
    fun `rejects a blank full name`() = runTest {
        assertReason(
            useCase()(validParams(fullName = "   ")),
            AuthValidationException.Reason.BLANK_FULL_NAME,
        )
    }

    @Test
    fun `rejects a password below the eight character minimum`() = runTest {
        // Seven characters: valid under the Phase 1 rule of six, rejected now.
        assertReason(
            useCase()(validParams(password = "abc1234", confirmPassword = "abc1234")),
            AuthValidationException.Reason.PASSWORD_TOO_SHORT,
        )
    }

    @Test
    fun `accepts a password of exactly the minimum length`() = runTest {
        coEvery { authRepository.signUp(any(), any(), any(), any()) } returns CREATION

        val result = useCase()(validParams(password = "abcd1234", confirmPassword = "abcd1234"))

        assertTrue(result is Result.Success)
    }

    @Test
    fun `rejects a password longer than BCrypt can hash without truncating`() = runTest {
        val tooLong = "a".repeat(AuthRules.MAX_PASSWORD_BYTES + 1)

        assertReason(
            useCase()(validParams(password = tooLong, confirmPassword = tooLong)),
            AuthValidationException.Reason.PASSWORD_TOO_LONG,
        )
    }

    @Test
    fun `rejects a confirmation that does not match`() = runTest {
        assertReason(
            useCase()(validParams(confirmPassword = "something-else")),
            AuthValidationException.Reason.PASSWORD_CONFIRMATION_MISMATCH,
        )
    }

    @Test
    fun `reports a blank password as blank rather than too short`() = runTest {
        assertReason(
            useCase()(validParams(password = "", confirmPassword = "")),
            AuthValidationException.Reason.BLANK_PASSWORD,
        )
    }

    @Test
    fun `surfaces a repository refusal as an Error carrying its reason`() = runTest {
        coEvery { authRepository.signUp(any(), any(), any(), any()) } throws
            AuthValidationException(AuthValidationException.Reason.ACCOUNT_ALREADY_EXISTS)

        assertReason(
            useCase()(validParams()),
            AuthValidationException.Reason.ACCOUNT_ALREADY_EXISTS,
        )
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
        cnic: String = "4210112345671",
        password: String = PASSWORD,
        confirmPassword: String = PASSWORD,
    ) = SignUpUseCase.Params(
        username = username,
        fullName = fullName,
        cnic = cnic,
        password = password,
        confirmPassword = confirmPassword,
    )

    private fun assertReason(
        result: Result<AccountCreation>,
        expected: AuthValidationException.Reason,
    ) {
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
            createdAt = 1_754_000_000_000L,
        )

        val CREATION = AccountCreation(account = ACCOUNT, recoveryCode = "ABCD-EFGH-JKLM")
    }
}
