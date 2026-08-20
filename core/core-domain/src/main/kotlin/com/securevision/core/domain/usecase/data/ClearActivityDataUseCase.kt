package com.securevision.core.domain.usecase.data

import com.securevision.core.common.dispatcher.DispatcherProvider
import com.securevision.core.domain.repository.AlertRepository
import com.securevision.core.domain.repository.DetectionEventRepository
import com.securevision.core.domain.repository.RecordingRepository
import com.securevision.core.domain.usecase.UseCase
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * Deletes everything the app has observed, but nobody it has learned.
 *
 * Alerts, detection events and recordings all regenerate from ordinary use, so
 * clearing them costs history and disk and nothing else. **Enrolled people are
 * deliberately out of scope.** A face embedding is biometric data held in exactly
 * one place with no backup, and folding "forget everyone you can recognise" into
 * a button labelled "clear data" would destroy it on a tap meant to free space.
 * Deleting a person stays on the People screen, where the confirmation names who
 * is being lost.
 *
 * @return `true` when the clear completed.
 */
class ClearActivityDataUseCase @Inject constructor(
    private val alertRepository: AlertRepository,
    private val detectionEventRepository: DetectionEventRepository,
    private val recordingRepository: RecordingRepository,
    dispatcherProvider: DispatcherProvider,
) : UseCase<Unit, Boolean>(dispatcherProvider.io) {

    override suspend fun execute(parameters: Unit): Boolean {
        // Everything strictly older than "now" is everything.
        val cutoff = System.currentTimeMillis() + FUTURE_MARGIN_MILLIS

        alertRepository.deleteOlderThan(cutoff)
        detectionEventRepository.deleteOlderThan(cutoff)

        // Recordings are deleted one at a time because each removal also unlinks
        // a video file; a bulk table wipe would orphan the largest files the app
        // writes.
        recordingRepository.getAll().first().forEach { recording ->
            recordingRepository.delete(recording.id)
        }

        return true
    }

    private companion object {
        /** Guards against a row stamped a moment in the future by clock skew. */
        const val FUTURE_MARGIN_MILLIS = 60_000L
    }
}
