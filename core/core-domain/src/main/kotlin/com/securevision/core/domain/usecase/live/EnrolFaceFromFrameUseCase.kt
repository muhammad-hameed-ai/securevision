package com.securevision.core.domain.usecase.live

import com.securevision.core.common.dispatcher.DispatcherProvider
import com.securevision.core.domain.engine.EnrolmentCapture
import com.securevision.core.domain.engine.FaceFrame
import com.securevision.core.domain.engine.FaceRecognitionEngine
import com.securevision.core.domain.engine.ProfilePhotoStore
import com.securevision.core.domain.repository.EnrolledProfileRepository
import com.securevision.core.domain.usecase.UseCase
import com.securevision.core.domain.usecase.profile.ProfileRules
import com.securevision.core.model.EnrolledProfile
import java.util.UUID
import javax.inject.Inject

/**
 * Enrols the face currently in frame.
 *
 * **Temporary, Phase 4.** The polished enrolment experience belongs to the
 * Profiles screen in a later phase. What must survive that rewrite is this
 * embedding path: it deliberately goes through the same
 * [FaceRecognitionEngine.embedForEnrolment] that recognition's detect → align →
 * embed uses. If the future Profiles screen grows its own second embedding path,
 * enrolments and queries will drift apart, and the symptom will be uniformly low
 * similarity — the exact bug this pipeline was built to eliminate.
 *
 * Its purpose here is to make the KNOWN path testable at all: without an enrolled
 * profile, matching, the margin rule and multi-frame voting would ship never
 * having been exercised against a real face.
 */
class EnrolFaceFromFrameUseCase @Inject constructor(
    private val engine: FaceRecognitionEngine,
    private val photoStore: ProfilePhotoStore,
    private val repository: EnrolledProfileRepository,
    dispatcherProvider: DispatcherProvider,
) : UseCase<EnrolFaceFromFrameUseCase.Params, EnrolledProfile>(dispatcherProvider.default) {

    /**
     * @property frame The frame to enrol from.
     * @property name Display name shown when this person is recognised.
     * @property age Age in years.
     */
    data class Params(
        val frame: FaceFrame,
        val name: String,
        val age: Int,
    )

    override suspend fun execute(parameters: Params): EnrolledProfile {
        val capture = engine.embedForEnrolment(parameters.frame)

        val success = when (capture) {
            is EnrolmentCapture.Success -> capture
            is EnrolmentCapture.Failure -> throw EnrolmentException(capture.reason)
        }

        // The stored photo is the aligned crop, not the wider frame: it shows
        // exactly what the model embedded, which makes a bad enrolment obvious by
        // looking at it rather than only by its match scores.
        val photoUri = photoStore.saveProfilePhoto(success.alignedFace)

        val profile = EnrolledProfile(
            id = UUID.randomUUID().toString(),
            name = parameters.name.trim(),
            age = parameters.age,
            photoUri = photoUri,
            embedding = success.embedding,
            isWatchlisted = false,
            createdAt = System.currentTimeMillis(),
        )

        // The same rules the Profiles screen enforces, shared rather than copied.
        ProfileRules.validate(profile)

        repository.add(profile)

        return profile
    }
}

/**
 * Raised when a frame could not produce a usable enrolment.
 *
 * Carries the engine's reason so the UI can tell the operator what to change —
 * step closer, face the camera, only one person in shot.
 *
 * @property reason Why the capture failed.
 */
class EnrolmentException(
    val reason: EnrolmentCapture.Failure.Reason,
) : Exception(reason.name)
