package com.securevision.core.data.security

import at.favre.lib.crypto.bcrypt.BCrypt

/**
 * BCrypt hashing for the password and the recovery code.
 *
 * BCrypt rather than a plain SHA family digest because it is deliberately slow
 * and salted per hash: an attacker who obtains the database still has to spend
 * the same work factor on every guess, and two operators choosing the same
 * password produce different stored values.
 *
 * The [cost] is a constructor parameter rather than a constant so tests can run
 * at a cheap factor. `PasswordHasherTest` separately asserts that the default
 * instance really emits a `$2a$12$` prefix, so a cheap test cost cannot hide a
 * misconfigured production one.
 *
 * @property cost BCrypt work factor; `2^cost` rounds.
 */
class PasswordHasher(private val cost: Int = DEFAULT_COST) {

    /**
     * Hashes a secret.
     *
     * @param plain Password or normalised recovery code. Converted to a
     *   `CharArray` for the call so the library never holds it as a `String`.
     * @return A self-describing BCrypt string containing the salt and cost.
     */
    fun hash(plain: String): String =
        BCrypt.withDefaults().hashToString(cost, plain.toCharArray())

    /**
     * Checks a secret against a stored hash.
     *
     * Uses BCrypt's own verifier, which compares in constant time. Never compare
     * hashes with `==`.
     *
     * @param plain Candidate secret.
     * @param hash Previously stored hash.
     * @return `true` when they match.
     */
    fun verify(plain: String, hash: String): Boolean =
        BCrypt.verifyer().verify(plain.toCharArray(), hash).verified

    companion object {
        /**
         * Work factor for production: 4,096 rounds.
         *
         * Roughly 300–800 ms on a mid-range phone, which is why every call is
         * confined to the IO dispatcher and the sign-in button shows a loading
         * state. Lowering this to make the UI feel faster would directly weaken
         * offline resistance.
         */
        const val DEFAULT_COST = 12
    }
}
