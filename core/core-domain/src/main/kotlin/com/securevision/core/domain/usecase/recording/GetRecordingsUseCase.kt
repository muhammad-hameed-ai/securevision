package com.securevision.core.domain.usecase.recording

import com.securevision.core.common.dispatcher.DispatcherProvider
import com.securevision.core.domain.repository.RecordingRepository
import com.securevision.core.domain.usecase.FlowUseCase
import com.securevision.core.model.Recording
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Streams recorded clips, newest first. */
class GetRecordingsUseCase @Inject constructor(
    private val recordingRepository: RecordingRepository,
    dispatcherProvider: DispatcherProvider,
) : FlowUseCase<Unit, List<Recording>>(dispatcherProvider.io) {

    override fun execute(parameters: Unit): Flow<List<Recording>> = recordingRepository.getAll()
}
