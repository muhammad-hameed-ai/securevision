package com.securevision.core.domain.engine

import com.securevision.core.model.AppSettings
import com.securevision.core.model.DetectionResult
import com.securevision.core.model.EnrolledProfile

/**
 * The on-device face recognition pipeline, as the presentation layer sees it.
 *
 * Declared here rather than in `:ml:ml-face` for the same reason every other
 * contract lives in the domain: `feature-live` must not depend on an `ml` module.
 * The detector, aligner, embedder, matcher, quality gate and voter all exist —
 * they are simply reached through this one seam, so a Composable cannot invoke
 * TFLite directly.
 */
interface FaceRecognitionEngine {

    /** Current readiness of the pipeline, including which delegate is running. */
    val status: EngineStatus

    /**
     * Loads the model and reports what it found.
     *
     * Called once when the live screen opens. Separate from construction so the
     * cost — reading and mapping a 45 MB asset — is paid when the camera is
     * actually about to be used, not when the Hilt graph is built.
     *
     * @param enrolledDimensions Embedding length of the stored profiles, so a
     *   model swap can be reported rather than silently producing meaningless
     *   scores. `null` when nothing is enrolled.
     * @return The resulting status.
     */
    suspend fun prepare(enrolledDimensions: Int?): EngineStatus

    /**
     * Runs the full pipeline over one frame.
     *
     * detect → quality gate → **align** → embed → match → vote. Alignment is not
     * optional and not skippable: feeding unaligned crops to the embedder is what
     * produced a similarity of roughly 0.23 for every face in the previous app.
     *
     * @param frame The frame to analyse.
     * @param profiles Enrolled profiles to match against. Pass an empty list and
     *   every face resolves to [com.securevision.core.model.MatchStatus.UNKNOWN].
     * @param settings Supplies the match threshold, margin and vote count.
     * @return One result per detected face that passed the quality gate.
     */
    suspend fun recognise(
        frame: FaceFrame,
        profiles: List<EnrolledProfile>,
        settings: AppSettings,
    ): List<DetectionResult>

    /**
     * Produces an embedding for the single most prominent face in a frame, for
     * enrolment.
     *
     * Deliberately the same detect → align → embed path [recognise] uses. Two
     * separate paths would eventually diverge, and an enrolment embedded
     * differently from the queries matched against it is itself a cause of the
     * near-constant low similarity this pipeline exists to avoid.
     *
     * @param frame The frame to enrol from.
     * @return The L2-normalised embedding, or a failure describing why not.
     */
    suspend fun embedForEnrolment(frame: FaceFrame): EnrolmentCapture

    /** Discards per-tracking-id voting history, e.g. when the camera is flipped. */
    fun resetTracking()
}

/**
 * Whether recognition can actually run, and on what.
 *
 * Modelled explicitly because "the model file is missing" must produce a clear
 * message rather than faces stuck in `PROCESSING` forever.
 */
sealed interface EngineStatus {

    /** The model has not been loaded yet. */
    data object Initialising : EngineStatus

    /**
     * Detection, alignment and embedding are all available.
     *
     * @property embeddingDimensions Output size read from the loaded model, not
     *   assumed. This is what stored profiles must match.
     * @property delegate Which accelerator is actually in use.
     */
    data class Ready(
        val embeddingDimensions: Int,
        val delegate: InferenceDelegate,
    ) : EngineStatus

    /**
     * Faces can still be detected, aligned and drawn, but not recognised.
     *
     * @property reason Why recognition is unavailable.
     */
    data class RecognitionUnavailable(val reason: Reason) : EngineStatus {

        /** Why the embedder is not usable. */
        enum class Reason {
            /** No model asset was found. Detection continues; recognition does not. */
            MODEL_NOT_INSTALLED,

            /** The asset exists but could not be loaded as a TFLite model. */
            MODEL_LOAD_FAILED,

            /**
             * The model's output dimension disagrees with the stored profiles'.
             *
             * The profiles were enrolled with a different model, so matching them
             * would compare vectors from two unrelated embedding spaces — which
             * yields plausible-looking but meaningless scores rather than an
             * obvious error. Re-enrolment is the fix.
             */
            EMBEDDING_DIMENSION_MISMATCH,
        }
    }
}

/** Which accelerator the embedder is running on. Reported so a silent CPU fallback is visible. */
enum class InferenceDelegate {
    /** Hardware GPU delegate. */
    GPU,

    /** Android Neural Networks API. */
    NNAPI,

    /** Plain CPU. Correct, but the slowest option. */
    CPU,
}

/** Outcome of trying to capture an embedding for enrolment. */
sealed interface EnrolmentCapture {

    /**
     * A usable face was captured.
     *
     * @property embedding L2-normalised embedding of the aligned crop.
     * @property alignedFace The 160×160 aligned crop, for the enrolment photo, so
     *   the stored image is the same thing the model actually saw.
     */
    data class Success(
        val embedding: FloatArray,
        val alignedFace: android.graphics.Bitmap,
    ) : EnrolmentCapture {

        /** Compares [embedding] by content; the generated equality would compare by reference. */
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Success) return false
            return embedding.contentEquals(other.embedding) && alignedFace == other.alignedFace
        }

        override fun hashCode(): Int = 31 * embedding.contentHashCode() + alignedFace.hashCode()
    }

    /**
     * No usable face was captured.
     *
     * @property reason Why, so the UI can tell the operator what to change.
     */
    data class Failure(val reason: Reason) : EnrolmentCapture {

        /** Why an enrolment capture did not produce an embedding. */
        enum class Reason {
            /** No face was found in the frame. */
            NO_FACE_DETECTED,

            /** More than one face was present; enrolment needs an unambiguous subject. */
            MULTIPLE_FACES,

            /** The face was too small, too turned, or too tilted to align reliably. */
            POOR_QUALITY,

            /** The detector could not locate all five alignment landmarks. */
            LANDMARKS_UNAVAILABLE,

            /** The embedder is not loaded, so no embedding can be produced. */
            MODEL_UNAVAILABLE,
        }
    }
}
