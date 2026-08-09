package com.securevision.ml.face

import android.graphics.Bitmap
import android.util.Log
import com.securevision.core.common.dispatcher.DispatcherProvider
import com.securevision.core.domain.engine.EngineStatus
import com.securevision.core.domain.engine.EnrolmentCapture
import com.securevision.core.domain.engine.FaceFrame
import com.securevision.core.domain.engine.FaceRecognitionEngine
import com.securevision.core.domain.engine.RecognisedFace
import com.securevision.core.model.AppSettings
import com.securevision.core.model.DetectionResult
import com.securevision.core.model.EnrolledProfile
import com.securevision.core.model.FaceDetection
import com.securevision.core.model.MatchStatus
import com.securevision.ml.face.align.FaceAligner
import com.securevision.ml.face.detect.MlKitFaceDetector
import com.securevision.ml.face.embed.FaceEmbedder
import com.securevision.ml.face.match.FaceMatcher
import com.securevision.ml.face.match.FaceQuality
import com.securevision.ml.face.match.FaceQualityGate
import com.securevision.ml.face.match.MatchOutcome
import com.securevision.ml.face.match.MultiFrameVoter
import com.securevision.ml.face.match.VoteResult
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.withContext

/**
 * Runs the six pipeline stages in order, on the CPU-bound dispatcher.
 *
 * The stage order in [recognise] is the whole point of this class, and
 * [FacePipelineStage] documents why each one is mandatory. In particular
 * **align** sits between the quality gate and the embedder and cannot be skipped:
 * there is no code path here that reaches [FaceEmbedder.embed] without having
 * gone through [FaceAligner.align] first.
 */
@Singleton
class FaceRecognitionEngineImpl @Inject constructor(
    private val detector: MlKitFaceDetector,
    private val aligner: FaceAligner,
    private val embedder: FaceEmbedder,
    private val matcher: FaceMatcher,
    private val qualityGate: FaceQualityGate,
    private val voter: MultiFrameVoter,
    private val dispatcherProvider: DispatcherProvider,
) : FaceRecognitionEngine {

    override val status: EngineStatus
        get() = embedder.status

    override suspend fun prepare(
        enrolledDimensions: Int?,
    ): EngineStatus = withContext(dispatcherProvider.default) {
        embedder.load()

        val modelDimensions = embedder.embeddingDimensions

        if (modelDimensions != null && enrolledDimensions != null &&
            modelDimensions != enrolledDimensions
        ) {
            Log.e(
                TAG,
                "model emits $modelDimensions dimensions but enrolled profiles hold " +
                    "$enrolledDimensions — these are different embedding spaces, so every " +
                    "score would be meaningless. Re-enrol to fix.",
            )
            // Reported rather than tolerated. Scoring across two embedding spaces
            // produces plausible-looking numbers, which is worse than refusing.
            return@withContext EngineStatus.RecognitionUnavailable(
                EngineStatus.RecognitionUnavailable.Reason.EMBEDDING_DIMENSION_MISMATCH,
            )
        }

        embedder.status
    }

    override suspend fun recognise(
        frame: FaceFrame,
        profiles: List<EnrolledProfile>,
        settings: AppSettings,
        retainAlignedCrops: Boolean,
    ): List<RecognisedFace> = withContext(dispatcherProvider.default) {
        // Stage 1 — detect.
        val detections = detector.detect(frame.bitmap)

        voter.retainOnly(detections.map { it.trackingId }.toSet())

        detections.map { detection ->
            resolve(detection, frame.bitmap, profiles, settings, retainAlignedCrops)
        }
    }

    override suspend fun embedForEnrolment(
        frame: FaceFrame,
    ): EnrolmentCapture = withContext(dispatcherProvider.default) {
        if (!embedder.isReady) {
            return@withContext EnrolmentCapture.Failure(
                EnrolmentCapture.Failure.Reason.MODEL_UNAVAILABLE,
            )
        }

        val detections = detector.detect(frame.bitmap)

        when {
            detections.isEmpty() -> return@withContext EnrolmentCapture.Failure(
                EnrolmentCapture.Failure.Reason.NO_FACE_DETECTED,
            )
            // Enrolment must be unambiguous: with two faces in shot there is no
            // way to know which one the operator meant to save.
            detections.size > 1 -> return@withContext EnrolmentCapture.Failure(
                EnrolmentCapture.Failure.Reason.MULTIPLE_FACES,
            )
        }

        val detection = detections.first()

        if (qualityGate.assess(detection) is FaceQuality.Rejected) {
            return@withContext EnrolmentCapture.Failure(
                EnrolmentCapture.Failure.Reason.POOR_QUALITY,
            )
        }

        val landmarks = detection.landmarks
            ?: return@withContext EnrolmentCapture.Failure(
                EnrolmentCapture.Failure.Reason.LANDMARKS_UNAVAILABLE,
            )

        // Identical align → embed path to recognition, on purpose. Two paths would
        // drift, and an enrolment embedded differently from the queries compared
        // against it is itself a cause of uniformly low similarity.
        val aligned = aligner.align(frame.bitmap, landmarks)
            ?: return@withContext EnrolmentCapture.Failure(
                EnrolmentCapture.Failure.Reason.LANDMARKS_UNAVAILABLE,
            )

        val embedding = embedder.embed(aligned)
            ?: return@withContext EnrolmentCapture.Failure(
                EnrolmentCapture.Failure.Reason.MODEL_UNAVAILABLE,
            )

        EnrolmentCapture.Success(embedding = embedding, alignedFace = aligned)
    }

    override fun resetTracking() {
        voter.reset()
    }

    /** Releases the detector and interpreter. */
    fun shutdown() {
        detector.close()
        embedder.close()
        voter.reset()
    }

    private fun resolve(
        detection: FaceDetection,
        frame: Bitmap,
        profiles: List<EnrolledProfile>,
        settings: AppSettings,
        retainAlignedCrops: Boolean,
    ): RecognisedFace {
        fun unresolved() = detection.toRecognised(
            status = MatchStatus.PROCESSING,
            profile = null,
            confidence = 0f,
            alignedCrop = null,
        )

        // Stage 2 — quality gate. A rejected face is still drawn, so the operator
        // can see it was noticed, but it never reaches the embedder.
        if (qualityGate.assess(detection) is FaceQuality.Rejected) return unresolved()

        val landmarks = detection.landmarks ?: return unresolved()

        // Recognition is off. Report PROCESSING rather than UNKNOWN so the UI
        // never claims a stranger it has not actually assessed.
        if (!embedder.isReady) return unresolved()

        // Stage 3 — ALIGN. Mandatory, and the only route to stage 4.
        val aligned = aligner.align(frame, landmarks) ?: return unresolved()

        // Carried out only on request. Attribute analysis reuses this exact crop
        // rather than re-detecting, which would cost a second detector pass and
        // could align marginally differently from the one that was recognised.
        val retainedCrop = aligned.takeIf { retainAlignedCrops }

        // Stage 4 — embed.
        val embedding = embedder.embed(aligned) ?: return unresolved()

        // Stage 5 — match.
        val outcome = matcher.findBestMatch(
            query = embedding,
            profiles = profiles,
            threshold = settings.confidenceThreshold,
            margin = settings.matchMargin,
        )

        val matchedProfile = (outcome as? MatchOutcome.Match)?.profile
        val score = when (outcome) {
            is MatchOutcome.Match -> outcome.score
            is MatchOutcome.NoMatch -> outcome.bestScore
        }

        // Stage 6 — vote.
        val vote = voter.record(
            trackingId = detection.trackingId,
            matchedProfileId = matchedProfile?.id,
            requiredAgreements = settings.voteFrames,
        )

        return when (vote) {
            is VoteResult.Known -> detection.toRecognised(
                status = MatchStatus.KNOWN,
                profile = profiles.firstOrNull { it.id == vote.profileId },
                confidence = score,
                alignedCrop = retainedCrop,
            )
            is VoteResult.Unknown -> detection.toRecognised(
                status = MatchStatus.UNKNOWN,
                profile = null,
                confidence = score,
                alignedCrop = retainedCrop,
            )
            VoteResult.Undecided -> detection.toRecognised(
                status = MatchStatus.PROCESSING,
                profile = null,
                confidence = score,
                alignedCrop = retainedCrop,
            )
        }
    }

    private fun FaceDetection.toRecognised(
        status: MatchStatus,
        profile: EnrolledProfile?,
        confidence: Float,
        alignedCrop: Bitmap?,
    ) = RecognisedFace(
        detection = DetectionResult(
            trackingId = trackingId,
            boundingBox = boundingBox,
            matchStatus = status,
            profileId = profile?.id,
            profileName = profile?.name,
            confidence = confidence,
        ),
        alignedCrop = alignedCrop,
        smilingProbability = smilingProbability,
    )

    private companion object {
        const val TAG = "FaceRecognitionEngine"
    }
}
