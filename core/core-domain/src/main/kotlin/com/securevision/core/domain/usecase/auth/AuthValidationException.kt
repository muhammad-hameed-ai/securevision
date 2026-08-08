package com.securevision.core.domain.usecase.auth

/**
 * Raised when credentials fail a domain rule before any network call is made.
 *
 * Carries a [reason] rather than a display message: this module has no access to
 * `strings.xml`, so the presentation layer maps the reason onto a localised
 * string. A hard-coded English message here could never be translated.
 *
 * @property reason Which rule was broken.
 */
class AuthValidationException(val reason: Reason) : Exception(reason.name) {

    /** The credential rules enforced at sign-up and login. */
    enum class Reason {
        /** Username was empty or whitespace. */
        BLANK_USERNAME,

        /** Username was shorter than [AuthRules.MIN_USERNAME_LENGTH]. */
        USERNAME_TOO_SHORT,

        /** Full name was empty or whitespace. */
        BLANK_FULL_NAME,

        /** Password was empty. */
        BLANK_PASSWORD,

        /** Password was shorter than [AuthRules.MIN_PASSWORD_LENGTH]. */
        PASSWORD_TOO_SHORT,

        /** CNIC did not contain exactly [AuthRules.CNIC_DIGIT_COUNT] digits. */
        INVALID_CNIC,
    }
}

/** Credential rules shared by sign-up and login. */
object AuthRules {

    /** Shortest accepted username. */
    const val MIN_USERNAME_LENGTH = 3

    /** Shortest accepted password, matching the Firebase Auth minimum. */
    const val MIN_PASSWORD_LENGTH = 6

    /** A Pakistani CNIC is thirteen digits, with or without separators. */
    const val CNIC_DIGIT_COUNT = 13

    /**
     * Strips separators from a CNIC so `"42101-1234567-1"` and `"4210112345671"`
     * are stored identically.
     *
     * @param cnic Raw user input.
     */
    fun normaliseCnic(cnic: String): String = cnic.filter(Char::isDigit)
}
