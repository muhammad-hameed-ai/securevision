package com.securevision.core.data.security

import com.securevision.core.domain.usecase.auth.AuthRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The recovery code is the only way back into an offline account, so it has to be
 * both unguessable and copyable by hand. These tests pin the second property;
 * the first comes from [java.security.SecureRandom] and is not unit-testable.
 */
class RecoveryCodeGeneratorTest {

    private val generator = RecoveryCodeGenerator()

    @Test
    fun `generates the specified number of characters once separators are stripped`() {
        val code = generator.generate()

        assertEquals(
            AuthRules.RECOVERY_CODE_LENGTH,
            AuthRules.normaliseRecoveryCode(code).length,
        )
    }

    @Test
    fun `groups the code for legibility`() {
        val code = generator.generate()

        // e.g. 7KP4-QW9M-2XHT
        assertTrue("unexpected shape: $code", code.matches(Regex("[A-Z0-9]{4}(-[A-Z0-9]{4}){2}")))
    }

    @Test
    fun `excludes every visually ambiguous character`() {
        val ambiguous = setOf('0', 'O', '1', 'I', 'L', 'U')

        repeat(times = 200) {
            val code = AuthRules.normaliseRecoveryCode(generator.generate())

            code.forEach { character ->
                assertTrue(
                    "generated an ambiguous character '$character' in $code",
                    character !in ambiguous,
                )
                assertTrue(
                    "generated an off-alphabet character '$character'",
                    character in RecoveryCodeGenerator.ALPHABET,
                )
            }
        }
    }

    @Test
    fun `does not repeat itself across many draws`() {
        val codes = List(size = 500) { generator.generate() }

        // Not a randomness proof — a broken generator returning a constant, or
        // one seeded identically per call, would fail here immediately.
        assertEquals(codes.size, codes.toSet().size)
    }

    @Test
    fun `normalisation accepts the code typed without dashes or in lower case`() {
        val code = generator.generate()

        val stripped = AuthRules.normaliseRecoveryCode(code.replace("-", "").lowercase())

        assertEquals(AuthRules.normaliseRecoveryCode(code), stripped)
    }
}
