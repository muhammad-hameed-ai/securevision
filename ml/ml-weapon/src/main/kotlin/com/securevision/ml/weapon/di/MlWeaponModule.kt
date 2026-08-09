package com.securevision.ml.weapon.di

import com.securevision.core.domain.engine.WeaponDetectionEngine
import com.securevision.ml.weapon.WeaponDetectionEngineImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds weapon detection to its domain contract.
 *
 * The only link between `feature-live` and this module; the feature declares no
 * dependency on it, and a compile probe proves the boundary holds.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class MlWeaponModule {

    @Binds
    @Singleton
    abstract fun bindWeaponDetectionEngine(
        implementation: WeaponDetectionEngineImpl,
    ): WeaponDetectionEngine
}
