package com.securevision.core.domain.usecase.recording

import com.securevision.core.common.dispatcher.DispatcherProvider
import com.securevision.core.domain.repository.RecordingRepository
import com.securevision.core.domain.usecase.UseCase
import com.securevision.core.model.Recording
import javax.inject.Inject

/**
 * Registers a clip that has finished writing.
 *
 * Called after the recorder has closed the file, never during capture: a row
 * pointing at a file still being written would show a zero-length clip in the
 * gallery and fail to play.
 */
class SaveRecordingUseCase @Inject constructor(
    private val recordingRepository: RecordingRepository,
    dispatcherProvider: DispatcherProvider,
) : UseCase<Recording, Unit>(dispatcherProvider.io) {

    override suspend fun execute(parameters: Recording) {
        recordingRepository.save(parameters)
    }
}
