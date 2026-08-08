package com.securevision.core.domain.usecase.dashboard

import com.securevision.core.common.dispatcher.DispatcherProvider
import com.securevision.core.domain.repository.DetectionEventRepository
import com.securevision.core.domain.usecase.FlowUseCase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Streams the total number of detections the pipeline has recorded. */
class GetDetectionEventCountUseCase @Inject constructor(
    private val detectionEventRepository: DetectionEventRepository,
    dispatcherProvider: DispatcherProvider,
) : FlowUseCase<Unit, Int>(dispatcherProvider.io) {

    override fun execute(parameters: Unit): Flow<Int> = detectionEventRepository.countAll()
}
