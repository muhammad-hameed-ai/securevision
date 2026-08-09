package com.securevision.core.data.session

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import app.cash.turbine.test
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Session persistence against a real preferences file.
 *
 * The "survives a restart" case matters most: it is what stops the operator being
 * asked to log in every time Android kills the process in the background.
 */
class SessionManagerTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    /** DataStore allows one active instance per file, so every store gets its own. */
    private val storeCounter = AtomicInteger()

    @Test
    fun `reads as signed out before anything is written`() = runTest {
        val session = sessionManager(backgroundScope)

        assertNull(session.currentSession.first())
        assertFalse(session.isLoggedIn.first())
    }

    @Test
    fun `login writes the uid`() = runTest {
        val session = sessionManager(backgroundScope)

        session.setSession(UID)

        assertEquals(UID, session.currentSession.first())
        assertTrue(session.isLoggedIn.first())
    }

    @Test
    fun `logout clears the uid`() = runTest {
        val session = sessionManager(backgroundScope)
        session.setSession(UID)

        session.clearSession()

        assertNull(session.currentSession.first())
        assertFalse(session.isLoggedIn.first())
    }

    @Test
    fun `isLoggedIn tracks the session as it changes`() = runTest {
        val session = sessionManager(backgroundScope)

        session.isLoggedIn.test {
            assertFalse(awaitItem())

            session.setSession(UID)
            assertTrue(awaitItem())

            session.clearSession()
            assertFalse(awaitItem())
        }
    }

    @Test
    fun `signing in as a different account replaces the previous uid`() = runTest {
        val session = sessionManager(backgroundScope)
        session.setSession(UID)

        session.setSession(OTHER_UID)

        assertEquals(OTHER_UID, session.currentSession.first())
    }

    @Test
    fun `the session survives the process being restarted`() = runTest {
        val file = File(temporaryFolder.root, "session.preferences_pb")

        // DataStore permits one active instance per file, so the first store's
        // scope is cancelled and joined before reopening — which is also a more
        // honest model of a process restart than opening two side by side.
        val firstProcess = Job()
        sessionManager(CoroutineScope(coroutineContext + firstProcess), file).setSession(UID)
        firstProcess.cancelAndJoin()

        val afterRestart = sessionManager(backgroundScope, file)

        assertEquals(UID, afterRestart.currentSession.first())
    }

    private fun sessionManager(
        scope: CoroutineScope,
        file: File = File(
            temporaryFolder.root,
            "session-${storeCounter.incrementAndGet()}.preferences_pb",
        ),
    ) = SessionManager(
        PreferenceDataStoreFactory.create(scope = scope, produceFile = { file }),
    )

    private companion object {
        const val UID = "3f0a1c88-0000-4000-8000-000000000001"
        const val OTHER_UID = "3f0a1c88-0000-4000-8000-000000000002"
    }
}
