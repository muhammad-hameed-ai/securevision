package com.securevision.core.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import app.cash.turbine.test
import com.securevision.core.common.dispatcher.DispatcherProvider
import com.securevision.core.data.database.SecureVisionDatabase
import com.securevision.core.data.database.dao.UserAccountDao
import com.securevision.core.data.security.PasswordHasher
import com.securevision.core.data.security.RecoveryCodeGenerator
import com.securevision.core.data.session.SessionManager
import com.securevision.core.domain.usecase.auth.AuthRules
import com.securevision.core.domain.usecase.auth.AuthValidationException
import com.securevision.core.model.AuthSession
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * End-to-end auth against a real in-memory database and a real preferences file.
 *
 * Nothing below the repository is mocked: the BCrypt hasher, the DAO and the
 * session store are all production classes, only at a cheap work factor.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AuthRepositoryImplTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val dispatchers = object : DispatcherProvider {
        override val io: CoroutineDispatcher = testDispatcher
        override val default: CoroutineDispatcher = testDispatcher
        override val main: CoroutineDispatcher = testDispatcher
    }

    private val hasher = PasswordHasher(cost = TEST_COST)

    /** DataStore allows one active instance per file, so every store gets its own. */
    private val storeCounter = AtomicInteger()

    private lateinit var database: SecureVisionDatabase
    private lateinit var dao: UserAccountDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            SecureVisionDatabase::class.java,
        ).allowMainThreadQueries().build()

        dao = database.userAccountDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // --- Sign-up -------------------------------------------------------------

    @Test
    fun `sign up stores a hash and never the plaintext password`() = runTest {
        val repository = repository(backgroundScope)

        repository.signUp(USERNAME, FULL_NAME, CNIC, PASSWORD)

        val stored = dao.getByUsername(USERNAME)!!
        assertNotEquals(PASSWORD, stored.passwordHash)
        assertFalse(stored.passwordHash.contains(PASSWORD))
        assertTrue(hasher.verify(PASSWORD, stored.passwordHash))
    }

    @Test
    fun `sign up returns a recovery code that verifies against the stored hash`() = runTest {
        val repository = repository(backgroundScope)

        val creation = repository.signUp(USERNAME, FULL_NAME, CNIC, PASSWORD)

        val stored = dao.getByUsername(USERNAME)!!
        assertNotEquals(creation.recoveryCode, stored.recoveryCodeHash)
        assertTrue(
            hasher.verify(
                AuthRules.normaliseRecoveryCode(creation.recoveryCode),
                stored.recoveryCodeHash,
            ),
        )
    }

    @Test
    fun `sign up signs the new account in`() = runTest {
        val session = sessionManager(backgroundScope)
        val repository = repository(backgroundScope, session)

        repository.signUp(USERNAME, FULL_NAME, CNIC, PASSWORD)

        session.currentSession.test {
            assertEquals(dao.getByUsername(USERNAME)!!.uid, awaitItem())
        }
    }

    @Test
    fun `a second sign up is refused because the app is single operator`() = runTest {
        val repository = repository(backgroundScope)
        repository.signUp(USERNAME, FULL_NAME, CNIC, PASSWORD)

        val failure = authFailureFrom {
            repository.signUp("someone-else", "Other Person", CNIC, PASSWORD)
        }

        assertEquals(AuthValidationException.Reason.ACCOUNT_ALREADY_EXISTS, failure.reason)
        assertEquals(1, dao.count())
    }

    // --- Login ---------------------------------------------------------------

    @Test
    fun `sign up then login with the correct password succeeds`() = runTest {
        val repository = repository(backgroundScope)
        repository.signUp(USERNAME, FULL_NAME, CNIC, PASSWORD)
        repository.logout()

        val account = repository.login(USERNAME, PASSWORD)

        assertEquals(USERNAME, account.username)
        assertEquals(FULL_NAME, account.fullName)
    }

    @Test
    fun `login with a wrong password fails with INVALID_CREDENTIALS`() = runTest {
        val repository = repository(backgroundScope)
        repository.signUp(USERNAME, FULL_NAME, CNIC, PASSWORD)

        val failure = authFailureFrom { repository.login(USERNAME, "wrong-password") }

        assertEquals(AuthValidationException.Reason.INVALID_CREDENTIALS, failure.reason)
    }

    @Test
    fun `login with an unknown username reports the same reason as a wrong password`() = runTest {
        val repository = repository(backgroundScope)
        repository.signUp(USERNAME, FULL_NAME, CNIC, PASSWORD)

        val failure = authFailureFrom { repository.login("nobody", PASSWORD) }

        // Identical reason on purpose: a distinct "no such user" would let anyone
        // holding the device enumerate valid usernames.
        assertEquals(AuthValidationException.Reason.INVALID_CREDENTIALS, failure.reason)
    }

    // --- Logout --------------------------------------------------------------

    @Test
    fun `logout clears the session but keeps the account`() = runTest {
        val session = sessionManager(backgroundScope)
        val repository = repository(backgroundScope, session)
        repository.signUp(USERNAME, FULL_NAME, CNIC, PASSWORD)

        repository.logout()

        session.currentSession.test { assertNull(awaitItem()) }
        assertEquals(1, dao.count())
    }

    // --- Recovery ------------------------------------------------------------

    @Test
    fun `the recovery code sets a new password`() = runTest {
        val repository = repository(backgroundScope)
        val creation = repository.signUp(USERNAME, FULL_NAME, CNIC, PASSWORD)
        repository.logout()

        repository.resetPassword(
            username = USERNAME,
            recoveryCode = AuthRules.normaliseRecoveryCode(creation.recoveryCode),
            newPassword = NEW_PASSWORD,
        )

        assertEquals(USERNAME, repository.login(USERNAME, NEW_PASSWORD).username)
    }

    @Test
    fun `the old password stops working after a reset`() = runTest {
        val repository = repository(backgroundScope)
        val creation = repository.signUp(USERNAME, FULL_NAME, CNIC, PASSWORD)

        repository.resetPassword(
            username = USERNAME,
            recoveryCode = AuthRules.normaliseRecoveryCode(creation.recoveryCode),
            newPassword = NEW_PASSWORD,
        )

        val failure = authFailureFrom { repository.login(USERNAME, PASSWORD) }
        assertEquals(AuthValidationException.Reason.INVALID_CREDENTIALS, failure.reason)
    }

    @Test
    fun `a wrong recovery code is refused`() = runTest {
        val repository = repository(backgroundScope)
        repository.signUp(USERNAME, FULL_NAME, CNIC, PASSWORD)

        val failure = authFailureFrom {
            repository.resetPassword(USERNAME, "22223333444M", NEW_PASSWORD)
        }

        assertEquals(AuthValidationException.Reason.INVALID_RECOVERY_CODE, failure.reason)
    }

    @Test
    fun `resetting the password does not sign the user in`() = runTest {
        val session = sessionManager(backgroundScope)
        val repository = repository(backgroundScope, session)
        val creation = repository.signUp(USERNAME, FULL_NAME, CNIC, PASSWORD)
        repository.logout()

        repository.resetPassword(
            username = USERNAME,
            recoveryCode = AuthRules.normaliseRecoveryCode(creation.recoveryCode),
            newPassword = NEW_PASSWORD,
        )

        // Logging in afterwards is what proves the new password is what they meant.
        session.currentSession.test { assertNull(awaitItem()) }
    }

    // --- Launch gate ---------------------------------------------------------

    @Test
    fun `auth state opens with Unknown then reports NoAccount`() = runTest {
        val repository = repository(backgroundScope)

        repository.observeAuthState().test {
            // Unknown first so the shell holds a splash rather than flashing login.
            assertEquals(AuthSession.Unknown, awaitItem())
            assertEquals(AuthSession.NoAccount, awaitItem())
        }
    }

    @Test
    fun `auth state reports SignedIn after sign up and SignedOut after logout`() = runTest {
        val repository = repository(backgroundScope)

        repository.signUp(USERNAME, FULL_NAME, CNIC, PASSWORD)

        val signedIn = repository.observeAuthState().firstSettled()
        assertTrue(signedIn is AuthSession.SignedIn)
        assertEquals(USERNAME, (signedIn as AuthSession.SignedIn).account.username)

        repository.logout()

        assertEquals(AuthSession.SignedOut, repository.observeAuthState().firstSettled())
    }

    @Test
    fun `getCurrentUser exposes no password material`() = runTest {
        val repository = repository(backgroundScope)
        repository.signUp(USERNAME, FULL_NAME, CNIC, PASSWORD)

        val account = repository.getCurrentUser().first { it != null }!!

        // UserAccount has no hash field at all; this asserts the rendered form
        // cannot leak one either.
        assertFalse(account.toString().contains(PASSWORD))
        assertFalse(account.toString().contains("\$2a\$"))
    }

    // --- Helpers -------------------------------------------------------------

    /**
     * Runs [block], expecting it to fail the auth rules.
     *
     * Written as a suspend helper rather than with `assertThrows`, which would
     * require nesting a second `runTest` inside the running one.
     */
    private suspend fun authFailureFrom(block: suspend () -> Unit): AuthValidationException =
        try {
            block()
            error("expected an AuthValidationException but the call succeeded")
        } catch (expected: AuthValidationException) {
            expected
        }

    /** First state after [AuthSession.Unknown] has passed. */
    private suspend fun Flow<AuthSession>.firstSettled(): AuthSession =
        first { it != AuthSession.Unknown }

    private fun sessionManager(scope: CoroutineScope) = SessionManager(
        PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = {
                File(
                    temporaryFolder.root,
                    "session-${storeCounter.incrementAndGet()}.preferences_pb",
                )
            },
        ),
    )

    private fun repository(
        scope: CoroutineScope,
        session: SessionManager = sessionManager(scope),
    ) = AuthRepositoryImpl(
        dao = dao,
        passwordHasher = hasher,
        recoveryCodeGenerator = RecoveryCodeGenerator(),
        sessionManager = session,
        dispatcherProvider = dispatchers,
    )

    private companion object {
        /** 2^4 rounds. Production is cost 12; PasswordHasherTest guards that. */
        const val TEST_COST = 4
        const val USERNAME = "hameed"
        const val FULL_NAME = "Muhammad Hameed"
        const val CNIC = "4210112345671"
        const val PASSWORD = "correct-horse"
        const val NEW_PASSWORD = "brand-new-secret"
    }
}
