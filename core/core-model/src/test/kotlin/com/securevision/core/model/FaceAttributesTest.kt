package com.securevision.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The null-versus-false contract.
 *
 * These read like trivial assertions, and that is the point: the distinction they
 * pin is the difference between an alert saying "this person was not wearing a
 * mask" and "nobody checked". The first is a claim the app cannot support unless
 * a classifier actually ran.
 */
class FaceAttributesTest {

    @Test
    fun `nothing is assessed by default`() {
        val attributes = FaceAttributes()

        assertNull(attributes.age)
        assertNull(attributes.gender)
        assertNull(attributes.emotion)
        assertNull(attributes.hasBeard)
        assertNull(attributes.hasMask)
        assertTrue(attributes.isEmpty)
    }

    @Test
    fun `NOT_ASSESSED carries no claims`() {
        assertTrue(FaceAttributes.NOT_ASSESSED.isEmpty)
        assertNull(FaceAttributes.NOT_ASSESSED.hasBeard)
        assertNull(FaceAttributes.NOT_ASSESSED.hasMask)
    }

    @Test
    fun `not assessed is distinct from assessed as absent`() {
        val notLooked = FaceAttributes(hasMask = null)
        val lookedAndFoundNone = FaceAttributes(hasMask = false)

        assertNotEquals(notLooked, lookedAndFoundNone)
        assertNull(notLooked.hasMask)
        assertEquals(false, lookedAndFoundNone.hasMask)
    }

    @Test
    fun `a single assessed attribute makes it non-empty`() {
        assertFalse(FaceAttributes(hasBeard = false).isEmpty)
        assertFalse(FaceAttributes(emotion = "neutral").isEmpty)
    }

    @Test
    fun `attributes can be partially assessed`() {
        // The realistic state: emotion available from the detector's smile score,
        // beard and mask awaiting models.
        val partial = FaceAttributes(emotion = "smiling")

        assertEquals("smiling", partial.emotion)
        assertNull(partial.hasBeard)
        assertNull(partial.hasMask)
        assertFalse(partial.isEmpty)
    }
}
