package com.securevision.core.domain.usecase.auth

/**
 * Raised when a credential operation fails a domain rule or an identity check.
 *
 * Carries a [reason] rather than a display message: this module has no access to
 * `strings.xml`, so the presentation layer maps the reason onto a localised
 * string. A hard-coded English message here could never be translated.
 *
 * @property reason Which rule was broken.
 */
class AuthValidationException(val reason: Reason) : Exception(reason.name) {

    /** The credential rules and identity checks enforced by the auth layer. */
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

        /** Password exceeded [AuthRules.MAX_PASSWORD_BYTES]; BCrypt would truncate it. */
        PASSWORD_TOO_LONG,

        /** The two password fields did not match. */
        PASSWORD_CONFIRMATION_MISMATCH,

        /** CNIC did not contain exactly [AuthRules.CNIC_DIGIT_COUNT] digits. */
        INVALID_CNIC,

        /** An account already exists; SecureVision is single-operator. */
        ACCOUNT_ALREADY_EXISTS,

        /** The chosen username is already in use. */
        USERNAME_TAKEN,

        /**
         * Sign-in failed.
         *
         * Used for both an unknown username and a wrong password, on purpose: a
         * distinct "no such user" reason would let anyone with the device
         * enumerate valid usernames.
         */
        INVALID_CREDENTIALS,

        /** No account exists on this device, so there is nothing to sign in to. */
        NO_ACCOUNT_EXISTS,

        /** The supplied recovery code did not match the stored hash. */
        INVALID_RECOVERY_CODE,
    }
}

/** Credential rules shared by sign-up, login and recovery. */
object AuthRules {

    /** Shortest accepted username. */
    const val MIN_USERNAME_LENGTH = 3

    /**
     * Shortest accepted password.
     *
     * Was 6 in Phase 1 only because that is the Firebase Auth floor. With the
     * account now offline and unrecoverable without the recovery code, there is
     * no reason to keep the weaker bound.
     */
    const val MIN_PASSWORD_LENGTH = 8

    /**
     * Longest accepted password, in bytes.
     *
     * BCrypt silently truncates its input at 72 bytes. Rejecting longer input is
     * honest; accepting it would tell the user their 100-character passphrase was
     * stored when only the first 72 bytes actually matter.
     */
    const val MAX_PASSWORD_BYTES = 72

    /** A Pakistani CNIC is thirteen digits, with or without separators. */
    const val CNIC_DIGIT_COUNT = 13

    /** Characters in a generated recovery code, excluding the group separators. */
    const val RECOVERY_CODE_LENGTH = 12

    /**
     * Strips separators from a CNIC so `"42101-1234567-1"` and `"4210112345671"`
     * are stored identically.
     *
     * @param cnic Raw user input.
     */
    fun normaliseCnic(cnic: String): String = cnic.filter(Char::isDigit)

    /**
     * Strips separators and normalises case so a recovery code can be typed with
     * or without its dashes, in any case.
     *
     * @param code Raw user input.
     */
    fun normaliseRecoveryCode(code: String): String =
        code.filter(Char::isLetterOrDigit).uppercase()
}
