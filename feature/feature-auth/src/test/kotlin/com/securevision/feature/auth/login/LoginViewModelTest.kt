package com.securevision.feature.auth.login

import app.cash.turbine.test
import com.securevision.core.common.dispatcher.DispatcherProvider
import com.securevision.core.domain.repository.AuthRepository
import com.securevision.core.domain.usecase.auth.AuthValidationException
import com.securevision.core.domain.usecase.auth.LoginUseCase
import com.securevision.core.model.UserAccount
import com.securevision.feature.auth.AuthUiState
import com.securevision.feature.auth.R
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Covers form state, submission gating and error mapping on the login screen. */
class LoginViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val authRepository = mockk<AuthRepository>(relaxed = true)

    @Before
    fun setUp() {
        // viewModelScope is main-dispatched.
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `starts idle with an empty, unsubmittable form`() {
        val viewModel = viewModel()

        assertEquals(AuthUiState.Idle, viewModel.uiState.value)
        assertEquals(LoginFormState(), viewModel.formState.value)
        assertFalse(viewModel.formState.value.canSubmit)
    }

    @Test
    fun `becomes submittable once both fields have content`() {
        val viewModel = viewModel()

        viewModel.onUsernameChange("hameed")
        assertFalse(viewModel.formState.value.canSubmit)

        viewModel.onPasswordChange("correct-horse")
        assertTrue(viewModel.formState.value.canSubmit)
    }

    @Test
    fun `a whitespace-only username does not enable submission`() {
        val viewModel = viewModel()

        viewModel.onUsernameChange("   ")
        viewModel.onPasswordChange("correct-horse")

        assertFalse(viewModel.formState.value.canSubmit)
    }

    @Test
    fun `reaches Success on a valid sign-in`() = runTest {
        coEvery { authRepository.login(any(), any()) } returns ACCOUNT
        val viewModel = viewModel()
        viewModel.onUsernameChange("hameed")
        viewModel.onPasswordChange("correct-horse")

        viewModel.submit()

        assertEquals(AuthUiState.Success, viewModel.uiState.value)
    }

    @Test
    fun `maps a rejected credential onto the localised message`() = runTest {
        coEvery { authRepository.login(any(), any()) } throws
            AuthValidationException(AuthValidationException.Reason.INVALID_CREDENTIALS)
        val viewModel = viewModel()
        viewModel.onUsernameChange("hameed")
        viewModel.onPasswordChange("wrong")

        viewModel.submit()

        assertEquals(
            AuthUiState.Error(R.string.auth_error_invalid_credentials),
            viewModel.uiState.value,
        )
    }

    @Test
    fun `editing the form clears a previous failure`() = runTest {
        coEvery { authRepository.login(any(), any()) } throws
            AuthValidationException(AuthValidationException.Reason.INVALID_CREDENTIALS)
        val viewModel = viewModel()
        viewModel.onUsernameChange("hameed")
        viewModel.onPasswordChange("wrong")
        viewModel.submit()

        viewModel.uiState.test {
            assertTrue(awaitItem() is AuthUiState.Error)

            viewModel.onPasswordChange("wrong-but-longer")

            assertEquals(AuthUiState.Idle, awaitItem())
        }
    }

    @Test
    fun `ignores a second submission while one is already running`() = runTest {
        // Never completes, so the ViewModel stays in Loading — standing in for the
        // few hundred milliseconds BCrypt actually takes at cost 12.
        coEvery { authRepository.login(any(), any()) } coAnswers {
            kotlinx.coroutines.awaitCancellation()
        }
        val viewModel = viewModel()
        viewModel.onUsernameChange("hameed")
        viewModel.onPasswordChange("correct-horse")

        viewModel.submit()
        viewModel.submit()
        viewModel.submit()

        assertEquals(AuthUiState.Loading, viewModel.uiState.value)
        coVerify(exactly = 1) { authRepository.login(any(), any()) }
    }

    private fun viewModel(): LoginViewModel {
        val dispatchers = object : DispatcherProvider {
            override val io: CoroutineDispatcher = testDispatcher
            override val default: CoroutineDispatcher = testDispatcher
            override val main: CoroutineDispatcher = testDispatcher
        }

        return LoginViewModel(LoginUseCase(authRepository, dispatchers))
    }

    private companion object {
        val ACCOUNT = UserAccount(
            uid = "uid-1",
            username = "hameed",
            fullName = "Muhammad Hameed",
            cnic = "4210112345671",
            createdAt = 1_754_000_000_000L,
        )
    }
}
