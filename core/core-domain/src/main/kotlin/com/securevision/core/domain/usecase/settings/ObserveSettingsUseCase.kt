package com.securevision.core.domain.usecase.settings

import com.securevision.core.common.dispatcher.DispatcherProvider
import com.securevision.core.domain.repository.SettingsRepository
import com.securevision.core.domain.usecase.FlowUseCase
import com.securevision.core.model.AppSettings
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Streams the user's settings.
 *
 * The live pipeline reads its match threshold, margin and vote count from here on
 * every frame, so changing any of them in Settings takes effect immediately
 * rather than on the next app launch — which is what makes tuning recognition
 * accuracy practical.
 */
class ObserveSettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
    dispatcherProvider: DispatcherProvider,
) : FlowUseCase<Unit, AppSettings>(dispatcherProvider.io) {

    override fun execute(parameters: Unit): Flow<AppSettings> = settingsRepository.settingsFlow
}
