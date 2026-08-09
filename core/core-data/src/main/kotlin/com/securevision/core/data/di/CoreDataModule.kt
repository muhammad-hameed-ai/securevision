package com.securevision.core.data.di

import com.securevision.core.data.repository.AlertRepositoryImpl
import com.securevision.core.data.repository.AuthRepositoryImpl
import com.securevision.core.data.repository.DetectionEventRepositoryImpl
import com.securevision.core.data.repository.EnrolledProfileRepositoryImpl
import com.securevision.core.data.repository.RecordingRepositoryImpl
import com.securevision.core.data.repository.SettingsRepositoryImpl
import com.securevision.core.data.storage.ProfilePhotoStoreImpl
import com.securevision.core.domain.engine.ProfilePhotoStore
import com.securevision.core.domain.repository.AlertRepository
import com.securevision.core.domain.repository.AuthRepository
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
 * Every contract is now bound here, including `AuthRepository` — the Phase 1 stub
 * in `:app` has been deleted. With Firebase dropped, no repository in this module
 * touches the network: the entire application state, account included, lives on
 * the device.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class CoreDataModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        implementation: AuthRepositoryImpl,
    ): AuthRepository

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

    /**
     * Lets the enrolment path persist a photo without any module above the data
     * layer gaining filesystem access.
     */
    @Binds
    @Singleton
    abstract fun bindProfilePhotoStore(
        implementation: ProfilePhotoStoreImpl,
    ): ProfilePhotoStore
}
