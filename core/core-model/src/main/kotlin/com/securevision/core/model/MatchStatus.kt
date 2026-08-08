package com.securevision.core.model

/**
 * Outcome of matching a detected face against the enrolled profiles.
 *
 * Drives the colour of the overlay box drawn on the live preview and the
 * severity of the resulting alert.
 */
enum class MatchStatus {

    /** Matched an enrolled profile with sufficient margin — drawn green. */
    KNOWN,

    /** No enrolled profile matched — drawn red, and raises an alarm. */
    UNKNOWN,

    /**
     * Not enough frames have been observed to commit to an identity yet.
     *
     * Multi-frame voting deliberately withholds a verdict until the evidence is
     * stable, which is what stops a single blurry frame flipping a known person
     * to red.
     */
    PROCESSING,
}
