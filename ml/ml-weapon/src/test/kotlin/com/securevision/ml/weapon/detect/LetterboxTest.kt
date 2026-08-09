package com.securevision.ml.weapon.detect

import com.securevision.core.model.BoundingBox
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Aspect-preserving fit into the model's square input, and the mapping back.
 *
 * The same class of bug as the overlay transform, with the same property that
 * makes it hard to spot: an error in the padding correction is smallest at the
 * centre of the image, so it reads as detector imprecision rather than a
 * coordinate bug.
 */
class LetterboxTest {

    @Test
    fun `a square frame needs no padding`() {
        val letterbox = Letterbox(sourceWidth = 640, sourceHeight = 640, targetSize = 640)

        assertEquals(1f, letterbox.scale, TOLERANCE)
        assertEquals(0f, letterbox.padX, TOLERANCE)
        assertEquals(0f, letterbox.padY, TOLERANCE)
    }

    @Test
    fun `scales by the smaller ratio so nothing is cropped`() {
        // 640x480 into 320: 320/640 = 0.5, 320/480 = 0.667. Must pick 0.5 —
        // picking the larger would crop, and a crop can remove the weapon.
        val letterbox = Letterbox(sourceWidth = 640, sourceHeight = 480, targetSize = 320)

        assertEquals(0.5f, letterbox.scale, TOLERANCE)
        assertEquals(320f, letterbox.scaledWidth, TOLERANCE)
        assertEquals(240f, letterbox.scaledHeight, TOLERANCE)
    }

    @Test
    fun `pads the short axis evenly on both sides`() {
        val letterbox = Letterbox(sourceWidth = 640, sourceHeight = 480, targetSize = 320)

        assertEquals(0f, letterbox.padX, TOLERANCE)
        assertEquals(40f, letterbox.padY, TOLERANCE)
    }

    @Test
    fun `pads horizontally for a portrait frame`() {
        val letterbox = Letterbox(sourceWidth = 480, sourceHeight = 640, targetSize = 320)

        assertEquals(0.5f, letterbox.scale, TOLERANCE)
        assertEquals(40f, letterbox.padX, TOLERANCE)
        assertEquals(0f, letterbox.padY, TOLERANCE)
    }

    @Test
    fun `a full-input box maps back to the whole frame`() {
        val letterbox = Letterbox(sourceWidth = 640, sourceHeight = 480, targetSize = 320)

        // The frame occupies y 40..280 of the 320 input.
        val mapped = letterbox.toFrameSpace(
            BoundingBox(left = 0f, top = 40f / 320f, right = 1f, bottom = 280f / 320f),
        )

        assertEquals(0f, mapped.left, TOLERANCE)
        assertEquals(0f, mapped.top, TOLERANCE)
        assertEquals(1f, mapped.right, TOLERANCE)
        assertEquals(1f, mapped.bottom, TOLERANCE)
    }

    @Test
    fun `a centred box stays centred after mapping back`() {
        val letterbox = Letterbox(sourceWidth = 640, sourceHeight = 480, targetSize = 320)

        val mapped = letterbox.toFrameSpace(BoundingBox(0.4f, 0.4f, 0.6f, 0.6f))

        assertEquals(0.5f, (mapped.left + mapped.right) / 2f, TOLERANCE)
        assertEquals(0.5f, (mapped.top + mapped.bottom) / 2f, TOLERANCE)
    }

    @Test
    fun `the padding offset is removed, not merely scaled`() {
        val letterbox = Letterbox(sourceWidth = 640, sourceHeight = 480, targetSize = 320)

        // Top of the actual image content, at y = 40 of 320.
        val mapped = letterbox.toFrameSpace(
            BoundingBox(left = 0f, top = 40f / 320f, right = 0.1f, bottom = 100f / 320f),
        )

        // Forgetting the offset would put this at 0.125 rather than 0.
        assertEquals(0f, mapped.top, TOLERANCE)
    }

    @Test
    fun `boxes are clamped to the frame`() {
        val letterbox = Letterbox(sourceWidth = 640, sourceHeight = 480, targetSize = 320)

        // A detection sitting in the padding, which is not part of the frame.
        val mapped = letterbox.toFrameSpace(BoundingBox(0f, 0f, 0.5f, 0.05f))

        assertTrue(mapped.top >= 0f)
        assertTrue(mapped.bottom >= 0f)
        assertTrue(mapped.bottom <= 1f)
    }

    @Test
    fun `a zero-sized frame does not divide by zero`() {
        val letterbox = Letterbox(sourceWidth = 0, sourceHeight = 0, targetSize = 320)

        assertEquals(1f, letterbox.scale, TOLERANCE)
    }

    private companion object {
        const val TOLERANCE = 1e-4f
    }
}
