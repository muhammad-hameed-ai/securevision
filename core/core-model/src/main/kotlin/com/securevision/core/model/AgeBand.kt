package com.securevision.core.model

/**
 * A coarse age grouping, derived from an estimate.
 *
 * Bands exist because a single number implies a precision no age model has: "27"
 * reads as a fact, "Young adult" reads as an estimate, and an estimate is what
 * this is. The raw figure is still carried alongside for anyone who wants it.
 *
 * **Only ever derived from a real estimate.** There is no rule here that guesses
 * a band from landmarks or face size — nothing ML Kit exposes supports that, and
 * a fabricated age in a security record is worse than a blank one.
 */
enum class AgeBand {

    /** Roughly 0–12. */
    CHILD,

    /** Roughly 13–19. */
    TEEN,

    /** Roughly 20–35. */
    YOUNG_ADULT,

    /** Roughly 36–55. */
    ADULT,

    /** Roughly 56 and over. */
    SENIOR,

    ;

    companion object {

        /**
         * Classifies an estimated age.
         *
         * @param age Estimated years, or `null` when nothing assessed it.
         * @return The band, or `null` — which the UI renders as "unknown", never
         *   as a guessed band.
         */
        fun of(age: Int?): AgeBand? = when {
            age == null || age < 0 -> null
            age <= CHILD_MAX -> CHILD
            age <= TEEN_MAX -> TEEN
            age <= YOUNG_ADULT_MAX -> YOUNG_ADULT
            age <= ADULT_MAX -> ADULT
            else -> SENIOR
        }

        private const val CHILD_MAX = 12
        private const val TEEN_MAX = 19
        private const val YOUNG_ADULT_MAX = 35
        private const val ADULT_MAX = 55
    }
}
