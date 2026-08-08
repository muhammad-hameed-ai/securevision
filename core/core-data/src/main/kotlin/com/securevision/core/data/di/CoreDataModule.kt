package com.securevision.core.data.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Binds the `core-domain` repository contracts to their on-device
 * implementations.
 *
 * Empty for Phase 1 by design: the interfaces exist, the implementations do not
 * yet. Phase 2 adds `@Binds` entries for the Room-backed profile, alert,
 * recording and detection-event repositories plus the DataStore-backed settings
 * repository; Phase 3 adds the Firebase-backed auth repository.
 *
 * Declaring the module now means the wiring point is fixed, and the Room,
 * Firebase and Hilt convention plugins on this module are exercised by every
 * build rather than only from Phase 2 onwards.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class CoreDataModule
