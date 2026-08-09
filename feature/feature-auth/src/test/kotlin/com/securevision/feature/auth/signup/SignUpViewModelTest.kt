package com.securevision.feature.auth.signup

import com.securevision.core.common.dispatcher.DispatcherProvider
import com.securevision.core.domain.repository.AuthRepository
import com.securevision.core.domain.usecase.auth.AuthValidationException
import com.securevision.core.domain.usecase.auth.SignUpUseCase
import com.securevision.core.model.AccountCreation
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Covers sign-up form state and the handling of the one-time recovery code.
 *
 * The recovery-code lifecycle matters as much as the sign-up itself: it is the
 * only moment that value exists outside a hash.
 */
class SignUpViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val authRepository = mockk<AuthRepository>(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `starts idle with no recovery code`() {
        val viewModel = viewModel()

        assertEquals(AuthUiState.Idle, viewModel.uiState.value)
        assertNull(viewModel.recoveryCode.value)
        assertFalse(viewModel.formState.value.canSubmit)
    }

    @Test
    fun `becomes submittable only when every field has content`() {
        val viewModel = viewModel().apply { fillValidForm() }

        assertTrue(viewModel.formState.value.canSubmit)

        viewModel.onCnicChange("")

        assertFalse(viewModel.formState.value.canSubmit)
    }

    @Test
    fun `exposes the recovery code on success`() = runTest {
        coEvery { authRepository.signUp(any(), any(), any(), any()) } returns CREATION
        val viewModel = viewModel().apply { fillValidForm() }

        viewModel.submit()

        assertEquals(AuthUiState.Success, viewModel.uiState.value)
        assertEquals(RECOVERY_CODE, viewModel.recoveryCode.value)
    }

    @Test
    fun `drops the recovery code once acknowledged`() = runTest {
        coEvery { authRepository.signUp(any(), any(), any(), any()) } returns CREATION
        val viewModel = viewModel().apply { fillValidForm() }
        viewModel.submit()

        viewModel.onRecoveryCodeAcknowledged()

        // Holding recoverable plaintext longer than necessary buys nothing.
        assertNull(viewModel.recoveryCode.value)
    }

    @Test
    fun `catches a password mismatch before reaching the repository`() = runTest {
        val viewModel = viewModel().apply {
            fillValidForm()
            onConfirmPasswordChange("something-else")
        }

        viewModel.submit()

        assertEquals(
            AuthUiState.Error(R.string.auth_error_password_mismatch),
            viewModel.uiState.value,
        )
        coVerify(exactly = 0) { authRepository.signUp(any(), any(), any(), any()) }
        assertNull(viewModel.recoveryCode.value)
    }

    @Test
    fun `rejects a password below the eight character minimum`() = runTest {
        val viewModel = viewModel().apply {
            fillValidForm()
            onPasswordChange("abc1234")
            onConfirmPasswordChange("abc1234")
        }

        viewModel.submit()

        assertEquals(
            AuthUiState.Error(R.string.auth_error_password_too_short),
            viewModel.uiState.value,
        )
    }

    @Test
    fun `surfaces the single-operator refusal`() = runTest {
        coEvery { authRepository.signUp(any(), any(), any(), any()) } throws
            AuthValidationException(AuthValidationException.Reason.ACCOUNT_ALREADY_EXISTS)
        val viewModel = viewModel().apply { fillValidForm() }

        viewModel.submit()

        assertEquals(
            AuthUiState.Error(R.string.auth_error_account_exists),
            viewModel.uiState.value,
        )
    }

    @Test
    fun `ignores a second submission while one is already running`() = runTest {
        coEvery { authRepository.signUp(any(), any(), any(), any()) } coAnswers {
            kotlinx.coroutines.awaitCancellation()
        }
        val viewModel = viewModel().apply { fillValidForm() }

        viewModel.submit()
        viewModel.submit()

        assertEquals(AuthUiState.Loading, viewModel.uiState.value)
        coVerify(exactly = 1) { authRepository.signUp(any(), any(), any(), any()) }
    }

    private fun SignUpViewModel.fillValidForm() {
        onUsernameChange("hameed")
        onFullNameChange("Muhammad Hameed")
        onCnicChange("42101-1234567-1")
        onPasswordChange("correct-horse")
        onConfirmPasswordChange("correct-horse")
    }

    private fun viewModel(): SignUpViewModel {
        val dispatchers = object : DispatcherProvider {
            override val io: CoroutineDispatcher = testDispatcher
            override val default: CoroutineDispatcher = testDispatcher
            override val main: CoroutineDispatcher = testDispatcher
        }

        return SignUpViewModel(SignUpUseCase(authRepository, dispatchers))
    }

    private companion object {
        const val RECOVERY_CODE = "7KP4-QW9M-2XHT"

        val ACCOUNT = UserAccount(
            uid = "uid-1",
            username = "hameed",
            fullName = "Muhammad Hameed",
            cnic = "4210112345671",
            createdAt = 1_754_000_000_000L,
        )

        val CREATION = AccountCreation(account = ACCOUNT, recoveryCode = RECOVERY_CODE)
    }
}
