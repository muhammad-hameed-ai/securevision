package com.securevision.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

/** Verifies the derived geometry the overlay renderer relies on. */
class BoundingBoxTest {

    @Test
    fun `derives width and height from edges`() {
        val box = BoundingBox(left = 0.2f, top = 0.1f, right = 0.6f, bottom = 0.5f)

        assertEquals(0.4f, box.width, TOLERANCE)
        assertEquals(0.4f, box.height, TOLERANCE)
    }

    @Test
    fun `derives centre point from edges`() {
        val box = BoundingBox(left = 0.2f, top = 0.1f, right = 0.6f, bottom = 0.5f)

        assertEquals(0.4f, box.centerX, TOLERANCE)
        assertEquals(0.3f, box.centerY, TOLERANCE)
    }

    @Test
    fun `computes area for a well formed box`() {
        val box = BoundingBox(left = 0f, top = 0f, right = 0.5f, bottom = 0.4f)

        assertEquals(0.2f, box.area, TOLERANCE)
    }

    @Test
    fun `reports zero area for an inverted box`() {
        val inverted = BoundingBox(left = 0.8f, top = 0.8f, right = 0.2f, bottom = 0.2f)

        assertEquals(0f, inverted.area, TOLERANCE)
    }

    @Test
    fun `reports zero area for a degenerate box`() {
        val degenerate = BoundingBox(left = 0.3f, top = 0.3f, right = 0.3f, bottom = 0.9f)

        assertEquals(0f, degenerate.area, TOLERANCE)
    }

    private companion object {
        const val TOLERANCE = 1e-5f
    }
}
