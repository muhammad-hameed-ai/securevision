package com.securevision.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the hand-written [EnrolledProfile.equals]/[EnrolledProfile.hashCode].
 *
 * These are the tests that would have failed had the class been left as a
 * `data class`: `FloatArray` compares by reference there, so two profiles built
 * from identical embeddings would be unequal.
 */
class EnrolledProfileTest {

    @Test
    fun `profiles with equal embedding contents are equal`() {
        val first = profile(embedding = floatArrayOf(0.1f, 0.2f, 0.3f))
        val second = profile(embedding = floatArrayOf(0.1f, 0.2f, 0.3f))

        assertEquals(first, second)
    }

    @Test
    fun `profiles with equal embedding contents share a hash code`() {
        val first = profile(embedding = floatArrayOf(0.1f, 0.2f, 0.3f))
        val second = profile(embedding = floatArrayOf(0.1f, 0.2f, 0.3f))

        assertEquals(first.hashCode(), second.hashCode())
    }

    @Test
    fun `profiles differing only in embedding contents are not equal`() {
        val first = profile(embedding = floatArrayOf(0.1f, 0.2f, 0.3f))
        val second = profile(embedding = floatArrayOf(0.1f, 0.2f, 0.4f))

        assertNotEquals(first, second)
    }

    @Test
    fun `profiles differing in a scalar field are not equal`() {
        assertNotEquals(profile(name = "Ayesha"), profile(name = "Bilal"))
    }

    @Test
    fun `copy preserves content equality`() {
        val original = profile(embedding = floatArrayOf(0.5f, 0.6f))

        assertEquals(original, original.copy())
    }

    @Test
    fun `copy replaces only the requested field`() {
        val original = profile(name = "Ayesha")

        val renamed = original.copy(name = "Ayesha Khan")

        assertEquals("Ayesha Khan", renamed.name)
        assertEquals(original.id, renamed.id)
        assertTrue(original.embedding.contentEquals(renamed.embedding))
    }

    @Test
    fun `toString omits raw embedding values`() {
        val rendered = profile(embedding = FloatArray(EnrolledProfile.SHIPPED_MODEL_EMBEDDING_SIZE)).toString()

        assertTrue(rendered.contains("512 dims"))
        assertTrue(rendered.contains("id=profile-1"))
    }

    private fun profile(
        id: String = "profile-1",
        name: String = "Ayesha",
        age: Int = 30,
        photoUri: String = "file:///data/profiles/profile-1.jpg",
        embedding: FloatArray = floatArrayOf(0.1f, 0.2f),
        isWatchlisted: Boolean = false,
        createdAt: Long = 1_700_000_000_000L,
    ) = EnrolledProfile(
        id = id,
        name = name,
        age = age,
        photoUri = photoUri,
        embedding = embedding,
        isWatchlisted = isWatchlisted,
        createdAt = createdAt,
    )
}
