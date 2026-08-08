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

/** Provides the preferences store backing [com.securevision.core.model.AppSettings]. */
@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    /**
     * Builds the preferences store.
     *
     * A corruption handler resets the file to empty rather than throwing on read.
     * Every setting falls back to its model default, so the worst case is that the
     * user's preferences revert — far better than an unrecoverable crash on launch.
     *
     * The scope uses a [SupervisorJob] so a failed write cannot cancel the store
     * for the remaining lifetime of the process.
     */
    @Provides
    @Singleton
    fun providePreferencesDataStore(
        @ApplicationContext context: Context,
        dispatcherProvider: DispatcherProvider,
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
        scope = CoroutineScope(dispatcherProvider.io + SupervisorJob()),
        produceFile = {
            context.preferencesDataStoreFile(Constants.Storage.SETTINGS_DATASTORE_NAME)
        },
    )
}
