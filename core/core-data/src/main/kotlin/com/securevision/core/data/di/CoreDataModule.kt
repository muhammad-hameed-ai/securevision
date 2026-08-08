package com.securevision.core.data.di

import com.securevision.core.data.repository.AlertRepositoryImpl
import com.securevision.core.data.repository.DetectionEventRepositoryImpl
import com.securevision.core.data.repository.EnrolledProfileRepositoryImpl
import com.securevision.core.data.repository.RecordingRepositoryImpl
import com.securevision.core.data.repository.SettingsRepositoryImpl
import com.securevision.core.domain.repository.AlertRepository
import com.securevision.core.domain.repository.DetectionEventRepository
import com.securevision.core.domain.repository.EnrolledProfileRepository
import com.securevision.core.domain.repository.RecordingRepository
import com.securevision.core.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds the `core-domain` repository contracts to their on-device implementations.
 *
 * `AuthRepository` is deliberately absent: it is still bound to the Phase 1 stub
 * in `:app` and moves here in Phase 3 with the Firebase implementation. That
 * split is why it is the only contract whose data does not stay on the device.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class CoreDataModule {

    @Binds
    @Singleton
    abstract fun bindEnrolledProfileRepository(
        implementation: EnrolledProfileRepositoryImpl,
    ): EnrolledProfileRepository

    @Binds
    @Singleton
    abstract fun bindAlertRepository(
        implementation: AlertRepositoryImpl,
    ): AlertRepository

    @Binds
    @Singleton
    abstract fun bindDetectionEventRepository(
        implementation: DetectionEventRepositoryImpl,
    ): DetectionEventRepository

    @Binds
    @Singleton
    abstract fun bindRecordingRepository(
        implementation: RecordingRepositoryImpl,
    ): RecordingRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        implementation: SettingsRepositoryImpl,
    ): SettingsRepository
}
