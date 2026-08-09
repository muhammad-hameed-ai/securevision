package com.securevision.ml.face

/**
 * The ordered stages of the on-device face recognition pipeline.
 *
 * Implemented by [FaceRecognitionEngineImpl], which runs them in exactly this
 * order. The list exists as a declared contract because a previous version of
 * this app returned a similarity of roughly 0.23 for every face it saw — known
 * and unknown alike — because [ALIGN] was missing and unaligned crops were fed
 * straight to the embedder. Every stage below is mandatory.
 */
enum class FacePipelineStage {

    /** ML Kit face detection with tracking enabled, producing a stable tracking id. */
    DETECT,

    /**
     * The quality gate.
     *
     * Placed before alignment rather than after so a face too small, too turned
     * or too tilted to embed well never reaches the model — and never gets a vote.
     */
    ASSESS_QUALITY,

    /**
     * Affine warp of the crop onto the canonical landmark positions.
     *
     * Non-negotiable. Skipping this collapses cosine similarity toward a constant
     * for every identity, which is indistinguishable from a broken model.
     */
    ALIGN,

    /** Inference over the aligned crop, followed by L2 normalisation. */
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
