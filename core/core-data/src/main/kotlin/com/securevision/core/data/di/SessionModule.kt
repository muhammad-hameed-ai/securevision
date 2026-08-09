package com.securevision.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.securevision.core.common.Constants
import com.securevision.core.common.dispatcher.DispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

/** Provides the preferences store holding the signed-in session. */
@Module
@InstallIn(SingletonComponent::class)
object SessionModule {

    /**
     * Builds the session store, in its own file.
     *
     * A corrupt file resolves to empty preferences, which reads as signed out.
     * Being asked to log in again is recoverable; crashing on launch is not.
     */
    @Provides
    @Singleton
    @SessionPreferences
    fun provideSessionDataStore(
        @ApplicationContext context: Context,
        dispatcherProvider: DispatcherProvider,
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
        scope = CoroutineScope(dispatcherProvider.io + SupervisorJob()),
        produceFile = {
            context.preferencesDataStoreFile(Constants.Storage.SESSION_DATASTORE_NAME)
        },
    )
}
