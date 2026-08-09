package com.securevision.core.data.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.securevision.core.data.di.SessionPreferences
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * Persists which account is signed in, across process death and reboots.
 *
 * Stores only the account's `uid` — an identifier, not a credential. Someone who
 * reads this file learns which account is active, not how to authenticate as it.
 *
 * @property dataStore The session preferences file.
 */
@Singleton
class SessionManager @Inject constructor(
    @param:SessionPreferences private val dataStore: DataStore<Preferences>,
) {

    /**
     * The signed-in account's uid, or `null` when signed out.
     *
     * An unreadable file surfaces as signed out rather than an exception: the
     * worst outcome is being asked to log in again, which is recoverable, whereas
     * throwing here would crash the app on launch.
     */
    val currentSession: Flow<String?> = dataStore.data
        .catch { throwable ->
            if (throwable is IOException) emit(emptyPreferences()) else throw throwable
        }
        .map { preferences -> preferences[Keys.LOGGED_IN_UID] }

    /** Whether a session is currently active. */
    val isLoggedIn: Flow<Boolean> = currentSession.map { it != null }

    /**
     * Records a successful sign-in.
     *
     * @param uid The account that signed in.
     */
    suspend fun setSession(uid: String) {
        dataStore.edit { preferences -> preferences[Keys.LOGGED_IN_UID] = uid }
    }

    /** Clears the session. The account row itself is untouched. */
    suspend fun clearSession() {
        dataStore.edit { preferences -> preferences.remove(Keys.LOGGED_IN_UID) }
    }

    private object Keys {
        val LOGGED_IN_UID = stringPreferencesKey("logged_in_uid")
    }
}
