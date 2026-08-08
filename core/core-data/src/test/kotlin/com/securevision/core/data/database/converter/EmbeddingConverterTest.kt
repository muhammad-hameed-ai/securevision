package com.securevision.core.data.database.converter

import com.securevision.core.model.EnrolledProfile
import kotlin.random.Random
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The most load-bearing test in the data layer.
 *
 * A converter that loses even one bit of a face embedding does not fail visibly —
 * it produces plausible-looking similarity scores that are quietly wrong for
 * every enrolled person. That is the same class of silent failure as the missing
 * alignment step that sank the previous version of this app, so the round trip
 * is asserted bit-exact rather than approximately.
 */
class EmbeddingConverterTest {

    private val converter = EmbeddingConverter()

    @Test
    fun `round trips a full FaceNet-512 embedding bit-exactly`() {
        val random = Random(seed = 20260809)
        val original = FloatArray(EnrolledProfile.EMBEDDING_SIZE) {
            random.nextFloat() * 2f - 1f
        }

        val restored = converter.toFloatArray(converter.fromFloatArray(original))

        assertArrayEquals(original, restored, 0f)
    }

    @Test
    fun `round trips the awkward values exactly`() {
        val original = floatArrayOf(
            0f,
            -0f,
            1f,
            -1f,
            Float.MIN_VALUE,
            Float.MAX_VALUE,
            -Float.MAX_VALUE,
            Float.MIN_VALUE / 2f, // subnormal
            0.1f,
            -0.123456789f,
        )

        val restored = converter.toFloatArray(converter.fromFloatArray(original))

        // Compared by raw bits, so negative zero is distinguished from positive zero
        // and no value is allowed to drift by a single ulp.
        original.indices.forEach { index ->
            assertEquals(
                "index $index",
                original[index].toRawBits(),
                restored[index].toRawBits(),
            )
        }
    }

    @Test
    fun `encodes four bytes per float`() {
        val embedding = FloatArray(EnrolledProfile.EMBEDDING_SIZE) { 0.5f }

        val encoded = converter.fromFloatArray(embedding)

        assertEquals(EnrolledProfile.EMBEDDING_SIZE * Float.SIZE_BYTES, encoded.size)
    }

    @Test
    fun `encodes little-endian regardless of platform default`() {
        // 1.0f is 0x3F800000; little-endian puts the low byte first.
        val encoded = converter.fromFloatArray(floatArrayOf(1.0f))

        assertArrayEquals(
            byteArrayOf(0x00, 0x00, 0x80.toByte(), 0x3F),
            encoded,
        )
    }

    @Test
    fun `round trips an empty embedding`() {
        val restored = converter.toFloatArray(converter.fromFloatArray(FloatArray(0)))

        assertTrue(restored.isEmpty())
    }

    @Test
    fun `rejects a blob that is not a whole number of floats`() {
        val malformed = byteArrayOf(1, 2, 3)

        val failure = assertThrows(IllegalArgumentException::class.java) {
            converter.toFloatArray(malformed)
        }

        assertTrue(failure.message!!.contains("not a multiple"))
    }
}
