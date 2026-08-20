package com.securevision.core.domain.usecase.recording

import com.securevision.core.common.dispatcher.DispatcherProvider
import com.securevision.core.domain.repository.RecordingRepository
import com.securevision.core.domain.usecase.UseCase
import javax.inject.Inject

/**
 * Deletes a clip and its metadata.
 *
 * The repository removes the video file alongside the row. Video is by far the
 * largest thing this app stores, so leaving orphaned files behind would quietly
 * consume the device.
 */
class DeleteRecordingUseCase @Inject constructor(
    private val recordingRepository: RecordingRepository,
    dispatcherProvider: DispatcherProvider,
) : UseCase<DeleteRecordingUseCase.Params, Unit>(dispatcherProvider.io) {

    /**
     * @property recordingId Identifier of the clip to remove.
     */
    data class Params(val recordingId: String)

    override suspend fun execute(parameters: Params) {
        require(parameters.recordingId.isNotBlank()) { "recordingId must not be blank" }

        recordingRepository.delete(parameters.recordingId)
    }
}
