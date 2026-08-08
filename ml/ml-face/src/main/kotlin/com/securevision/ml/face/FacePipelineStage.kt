package com.securevision.ml.face

/**
 * The ordered stages of the on-device face recognition pipeline, implemented in
 * Phase 4.
 *
 * Declared now, before any of it is written, because the stage list is the part
 * that must not drift. A previous version of this app returned a similarity of
 * roughly 0.23 for every face it saw — known and unknown alike — because
 * [ALIGN] was missing and unaligned crops were fed straight to the embedder.
 * Every stage below is mandatory.
 */
enum class FacePipelineStage {

    /** ML Kit face detection with tracking enabled, producing a stable tracking id. */
    DETECT,

    /** Five-point landmark extraction: both eyes, nose tip, both mouth corners. */
    EXTRACT_LANDMARKS,

    /**
     * Affine warp of the crop onto the model's canonical landmark positions.
     *
     * Non-negotiable. Skipping this collapses cosine similarity toward a constant
     * for every identity, which is indistinguishable from a broken model.
     */
    ALIGN,

    /** FaceNet-512 inference over the aligned crop, followed by L2 normalisation. */
    EMBED,

    /**
     * Cosine similarity against every enrolled embedding, accepting a match only
     * when the best score clears the threshold *and* leads the runner-up by the
     * configured margin.
     */
    MATCH,

    /**
     * Majority vote across consecutive frames for one tracking id, so a single
     * blurred frame cannot flip a known person to unknown.
     */
    VOTE,
}

/** Provisional on-device model metadata for the embedder; confirmed in Phase 4. */
internal object FaceModelSpec {

    /** Asset name of the FaceNet-512 TFLite model. */
    const val ASSET_NAME = "facenet_512.tflite"

    /** Square input edge, in pixels, the model expects after alignment. */
    const val INPUT_SIZE = 160

    /** Output embedding dimensionality. */
    const val EMBEDDING_SIZE = 512
}
