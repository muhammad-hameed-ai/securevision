package com.securevision.core.data.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the guarantees the account store depends on.
 *
 * Runs at a cheap work factor so the suite stays fast, with one test asserting
 * the production default separately — otherwise a misconfigured production cost
 * could hide behind the test cost forever.
 */
class PasswordHasherTest {

    private val hasher = PasswordHasher(cost = TEST_COST)

    @Test
    fun `hash does not contain the plaintext`() {
        val hash = hasher.hash(PASSWORD)

        assertFalse(hash.contains(PASSWORD))
        assertNotEquals(PASSWORD, hash)
    }

    @Test
    fun `verify accepts the correct password`() {
        val hash = hasher.hash(PASSWORD)

        assertTrue(hasher.verify(PASSWORD, hash))
    }

    @Test
    fun `verify rejects a wrong password`() {
        val hash = hasher.hash(PASSWORD)

        assertFalse(hasher.verify("not-the-password", hash))
    }

    @Test
    fun `verify rejects a password differing only in case`() {
        val hash = hasher.hash(PASSWORD)

        assertFalse(hasher.verify(PASSWORD.uppercase(), hash))
    }

    @Test
    fun `the same password hashes differently every time`() {
        val first = hasher.hash(PASSWORD)
        val second = hasher.hash(PASSWORD)

        // Per-hash salt. Two operators choosing the same password must not produce
        // the same stored value, or one cracked hash would reveal both.
        assertNotEquals(first, second)
        assertTrue(hasher.verify(PASSWORD, first))
        assertTrue(hasher.verify(PASSWORD, second))
    }

    @Test
    fun `verify rejects a malformed hash instead of throwing`() {
        assertFalse(hasher.verify(PASSWORD, "not-a-bcrypt-hash"))
    }

    @Test
    fun `the production default really is cost 12`() {
        // Guards the whole point of the injected cost: tests run cheap, production
        // must not. The prefix encodes the algorithm variant and the work factor.
        val productionHash = PasswordHasher().hash(PASSWORD)

        assertTrue(
            "expected a \$2a\$12\$ prefix but was ${productionHash.take(7)}",
            productionHash.startsWith("\$2a\$12\$"),
        )
        assertEquals(PasswordHasher.DEFAULT_COST, 12)
    }

    @Test
    fun `hashes the recovery code the same way as a password`() {
        val code = "7KP4QW9M2XHT"

        val hash = hasher.hash(code)

        assertTrue(hasher.verify(code, hash))
        assertFalse(hasher.verify("7KP4QW9M2XHU", hash))
    }

    private companion object {
        /** 2^4 rounds — fast enough for a unit suite, useless in production. */
        const val TEST_COST = 4
        const val PASSWORD = "correct-horse-battery"
    }
}
