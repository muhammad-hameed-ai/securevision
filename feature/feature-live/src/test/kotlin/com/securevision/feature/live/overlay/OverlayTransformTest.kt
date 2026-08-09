package com.securevision.feature.live.overlay

import com.securevision.core.model.BoundingBox
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The overlay maths, against hand-computed expectations.
 *
 * Worth testing precisely because the failure mode is subtle: a wrong scale
 * formula still puts boxes on the face at the centre of the screen, and only
 * drifts at the edges — which is easy to dismiss as detector jitter on a device.
 */
class OverlayTransformTest {

    @Test
    fun `no scaling needed when the frame already matches the viewport`() {
        val transform = transform(analysisWidth = 100, analysisHeight = 200, viewWidth = 100f, viewHeight = 200f)

        assertEquals(1f, transform.scale, TOLERANCE)
        assertEquals(0f, transform.offsetX, TOLERANCE)
        assertEquals(0f, transform.offsetY, TOLERANCE)
    }

    @Test
    fun `fills the viewport using the larger ratio, not the smaller`() {
        // 480x640 analysis into a 1080x1920 view: 1080/480 = 2.25, 1920/640 = 3.0.
        // FILL_CENTER must pick 3.0 and crop the width; picking 2.25 would letterbox.
        val transform = transform(480, 640, 1080f, 1920f)

        assertEquals(3.0f, transform.scale, TOLERANCE)
    }

    @Test
    fun `centres the crop, so the overflow is split evenly`() {
        val transform = transform(480, 640, 1080f, 1920f)

        // Scaled width is 1440 against a 1080 viewport, so 180 hangs off each side.
        assertEquals(-180f, transform.offsetX, TOLERANCE)
        assertEquals(0f, transform.offsetY, TOLERANCE)
    }

    @Test
    fun `projects a centred box to the centre of the view`() {
        val transform = transform(480, 640, 1080f, 1920f)

        val rect = transform.project(BoundingBox(0.4f, 0.4f, 0.6f, 0.6f))

        assertEquals(540f, (rect.left + rect.right) / 2f, TOLERANCE)
        assertEquals(960f, (rect.top + rect.bottom) / 2f, TOLERANCE)
    }

    @Test
    fun `projects a full-frame box to cover the whole view`() {
        val transform = transform(480, 640, 1080f, 1920f)

        val rect = transform.project(BoundingBox(0f, 0f, 1f, 1f))

        // Horizontally the frame overhangs, vertically it fits exactly.
        assertEquals(-180f, rect.left, TOLERANCE)
        assertEquals(1260f, rect.right, TOLERANCE)
        assertEquals(0f, rect.top, TOLERANCE)
        assertEquals(1920f, rect.bottom, TOLERANCE)
    }

    @Test
    fun `back camera does not mirror`() {
        val transform = transform(480, 640, 1080f, 1920f, isFrontCamera = false)

        val rect = transform.project(BoundingBox(0.0f, 0.1f, 0.2f, 0.3f))

        // A box on the left of the frame stays on the left.
        assertTrue("expected a left-side box, got ${rect.left}", rect.left < 540f)
    }

    @Test
    fun `front camera mirrors horizontally`() {
        val front = transform(480, 640, 1080f, 1920f, isFrontCamera = true)
        val back = transform(480, 640, 1080f, 1920f, isFrontCamera = false)
        val box = BoundingBox(0.0f, 0.1f, 0.2f, 0.3f)

        val mirrored = front.project(box)
        val plain = back.project(box)

        assertEquals(1080f - plain.right, mirrored.left, TOLERANCE)
        assertEquals(1080f - plain.left, mirrored.right, TOLERANCE)
    }

    @Test
    fun `mirroring keeps left smaller than right`() {
        val transform = transform(480, 640, 1080f, 1920f, isFrontCamera = true)

        val rect = transform.project(BoundingBox(0.1f, 0.1f, 0.4f, 0.5f))

        // Reflection swaps the edges; failing to re-order produces a negative
        // width and a box that never draws.
        assertTrue("left ${rect.left} should be < right ${rect.right}", rect.left < rect.right)
        assertTrue(rect.width > 0f)
    }

    @Test
    fun `mirroring leaves vertical position untouched`() {
        val front = transform(480, 640, 1080f, 1920f, isFrontCamera = true)
        val back = transform(480, 640, 1080f, 1920f, isFrontCamera = false)
        val box = BoundingBox(0.1f, 0.25f, 0.4f, 0.55f)

        assertEquals(back.project(box).top, front.project(box).top, TOLERANCE)
        assertEquals(back.project(box).bottom, front.project(box).bottom, TOLERANCE)
    }

    @Test
    fun `a landscape frame in a portrait view crops vertically`() {
        // 640x480 into 1080x1920: 1080/640 = 1.6875, 1920/480 = 4.0 -> 4.0.
        val transform = transform(640, 480, 1080f, 1920f)

        assertEquals(4.0f, transform.scale, TOLERANCE)
        assertEquals((1080f - 2560f) / 2f, transform.offsetX, TOLERANCE)
        assertEquals(0f, transform.offsetY, TOLERANCE)
    }

    @Test
    fun `a zero-sized frame does not divide by zero`() {
        val transform = transform(0, 0, 1080f, 1920f)

        assertEquals(1f, transform.scale, TOLERANCE)
    }

    private fun transform(
        analysisWidth: Int,
        analysisHeight: Int,
        viewWidth: Float,
        viewHeight: Float,
        isFrontCamera: Boolean = false,
    ) = OverlayTransform(
        analysisWidth = analysisWidth,
        analysisHeight = analysisHeight,
        viewWidth = viewWidth,
        viewHeight = viewHeight,
        isFrontCamera = isFrontCamera,
    )

    private companion object {
        const val TOLERANCE = 1e-3f
    }
}
