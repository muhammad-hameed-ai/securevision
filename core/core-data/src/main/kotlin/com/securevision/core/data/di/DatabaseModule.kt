package com.securevision.core.data.di

import android.content.Context
import androidx.room.Room
import com.securevision.core.common.Constants
import com.securevision.core.data.database.SecureVisionDatabase
import com.securevision.core.data.database.dao.AlertDao
import com.securevision.core.data.database.dao.DetectionEventDao
import com.securevision.core.data.database.dao.EnrolledProfileDao
import com.securevision.core.data.database.dao.RecordingDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Provides the Room database and each of its DAOs. */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Builds the database.
     *
     * `fallbackToDestructiveMigration` is deliberately **not** set. Dropping the
     * tables on a schema change would silently wipe every enrolled profile — data
     * the user cannot recover, since it never left the device. A missing migration
     * must fail loudly instead.
     */
    @Provides
    @Singleton
    fun provideSecureVisionDatabase(
        @ApplicationContext context: Context,
    ): SecureVisionDatabase = Room.databaseBuilder(
        context = context,
        klass = SecureVisionDatabase::class.java,
        name = Constants.Storage.DATABASE_NAME,
    ).build()

    @Provides
    fun provideEnrolledProfileDao(database: SecureVisionDatabase): EnrolledProfileDao =
        database.enrolledProfileDao()

    @Provides
    fun provideAlertDao(database: SecureVisionDatabase): AlertDao = database.alertDao()

    @Provides
    fun provideDetectionEventDao(database: SecureVisionDatabase): DetectionEventDao =
        database.detectionEventDao()

    @Provides
    fun provideRecordingDao(database: SecureVisionDatabase): RecordingDao = database.recordingDao()
}
