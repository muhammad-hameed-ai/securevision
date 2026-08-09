package com.securevision.core.data.security

import com.securevision.core.domain.usecase.auth.AuthRules
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Generates the one-time recovery code issued at sign-up.
 *
 * This code is the only way back into an offline account whose password has been
 * forgotten, so two properties matter: it must be unguessable, and it must
 * survive being copied onto paper by hand.
 *
 * Unguessable comes from [SecureRandom] — never `kotlin.random.Random`, which is
 * seeded predictably and is not a cryptographic source. Twelve characters from a
 * thirty-symbol alphabet is about 59 bits of entropy, which is far beyond what
 * an attacker could work through against a BCrypt cost-12 hash.
 *
 * Transcribable comes from the alphabet: digits `0` and `1` and letters
 * `I`, `L`, `O`, `U` are excluded, so there is no character pair a person can
 * confuse when reading their own handwriting back.
 */
@Singleton
class RecoveryCodeGenerator @Inject constructor() {

    private val secureRandom = SecureRandom()

    /**
     * Produces a code such as `7KP4-QW9M-2XHT`.
     *
     * The dashes are presentation only —
     * [AuthRules.normaliseRecoveryCode] strips them before hashing, so the user
     * may type the code with or without them, in any case.
     */
    fun generate(): String {
        val characters = CharArray(AuthRules.RECOVERY_CODE_LENGTH) {
            ALPHABET[secureRandom.nextInt(ALPHABET.length)]
        }

        return characters
            .concatToString()
            .chunked(GROUP_SIZE)
            .joinToString(GROUP_SEPARATOR)
    }

    companion object {
        /** Thirty symbols, with every visually ambiguous character removed. */
        const val ALPHABET = "23456789ABCDEFGHJKMNPQRSTVWXYZ"

        private const val GROUP_SIZE = 4
        private const val GROUP_SEPARATOR = "-"
    }
}
