package com.securevision.core.alerting.di

import com.securevision.core.alerting.audio.AudioTrackAlarmPlayer
import com.securevision.core.alerting.notify.AlertNotifierImpl
import com.securevision.core.domain.alerting.AlarmPlayer
import com.securevision.core.domain.alerting.AlertNotifier
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds the alerting contracts to their platform implementations.
 *
 * Both are singletons because both own hardware state: the player holds an
 * `AudioTrack` and an audio-focus request, and a second instance would leave the
 * first one's focus abandoned and its alarm unstoppable.
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class CoreAlertingModule {

    @Binds
    @Singleton
    abstract fun bindAlarmPlayer(impl: AudioTrackAlarmPlayer): AlarmPlayer

    @Binds
    @Singleton
    abstract fun bindAlertNotifier(impl: AlertNotifierImpl): AlertNotifier
}
