package com.securevision.core.model

/**
 * What an enrolled person is permitted, as the operator has classified them.
 *
 * This is a label the operator applies and the app displays — it does not gate
 * anything the app itself does. Recognition treats every enrolled person the same
 * way; what changes is what the person reviewing an alert is told.
 *
 * Distinct from [EnrolledProfile.isWatchlisted], which is orthogonal: a VIP can be
 * watchlisted (tell me when they arrive) and so can a restricted person (tell me
 * because they should not be here). Level says *who they are*, the watchlist flag
 * says *whether to escalate*.
 */
enum class AccessLevel {

    /** The default. An ordinary known person. */
    STANDARD,

    /** Known, but not expected or not permitted in this area. */
    RESTRICTED,

    /** Known and prioritised. */
    VIP,

    ;

    companion object {

        /** What existing profiles are assigned when the column is introduced. */
        val DEFAULT: AccessLevel = STANDARD

        /**
         * Parses a stored name, falling back to [DEFAULT].
         *
         * Lenient on purpose, unlike the alert enums: an unrecognised access level
         * costs a mislabelled badge, whereas refusing to load the row would hide a
         * person the app can otherwise still recognise perfectly well.
         *
         * @param value Stored name, or `null` for a row written before this
         *   column existed.
         */
        fun fromStorage(value: String?): AccessLevel =
            entries.firstOrNull { level -> level.name == value } ?: DEFAULT
    }
}
