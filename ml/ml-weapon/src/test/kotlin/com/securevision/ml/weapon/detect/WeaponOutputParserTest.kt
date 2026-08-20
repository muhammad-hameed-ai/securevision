package com.securevision.ml.weapon.detect

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Output parsing against synthetic tensors.
 *
 * Reading a channels-major tensor as anchor-major, or the reverse, produces
 * detections that are numerically valid and completely wrong — boxes in plausible
 * places with plausible scores, describing nothing. That is why the layout is
 * inferred from the shape and tested both ways round.
 */
class WeaponOutputParserTest {

    private val classCount = WeaponClassMap.EXPECTED_CLASS_COUNT
    private val channels = BOX_VALUES + classCount

    @Test
    fun `parses a channels-major tensor`() {
        val anchors = 3
        val output = FloatArray(channels * anchors)

        // Anchor 1: a weapon (the only class) at the centre, half size, score 0.9.
        fun set(channel: Int, anchor: Int, value: Float) {
            output[channel * anchors + anchor] = value
        }
        set(0, 1, 0.5f)
        set(1, 1, 0.5f)
        set(2, 1, 0.4f)
        set(3, 1, 0.4f)
        set(BOX_VALUES + 0, 1, 0.9f)

        val parsed = WeaponOutputParser.parse(
            output = output,
            shape = intArrayOf(1, channels, anchors),
            confidenceThreshold = 0.5f,
            inputSize = 640,
        )

        assertEquals(1, parsed.size)
        assertEquals("Weapon", parsed.first().weaponType)
        assertEquals(0.9f, parsed.first().confidence, TOLERANCE)
        assertEquals(0.3f, parsed.first().boundingBox.left, TOLERANCE)
        assertEquals(0.7f, parsed.first().boundingBox.right, TOLERANCE)
    }

    @Test
    fun `parses an anchor-major tensor to the same result`() {
        val anchors = 3
        val output = FloatArray(anchors * channels)

        fun set(anchor: Int, channel: Int, value: Float) {
            output[anchor * channels + channel] = value
        }
        set(1, 0, 0.5f)
        set(1, 1, 0.5f)
        set(1, 2, 0.4f)
        set(1, 3, 0.4f)
        set(1, BOX_VALUES + 0, 0.9f)

        val parsed = WeaponOutputParser.parse(
            output = output,
            shape = intArrayOf(1, anchors, channels),
            confidenceThreshold = 0.5f,
            inputSize = 640,
        )

        assertEquals(1, parsed.size)
        assertEquals("Weapon", parsed.first().weaponType)
        assertEquals(0.3f, parsed.first().boundingBox.left, TOLERANCE)
    }

    @Test
    fun `drops anchors below the confidence threshold`() {
        val anchors = 2
        val output = FloatArray(channels * anchors)
        output[0 * anchors + 0] = 0.5f
        output[1 * anchors + 0] = 0.5f
        output[2 * anchors + 0] = 0.2f
        output[3 * anchors + 0] = 0.2f
        output[(BOX_VALUES + 0) * anchors + 0] = 0.4f

        val parsed = WeaponOutputParser.parse(
            output = output,
            shape = intArrayOf(1, channels, anchors),
            confidenceThreshold = 0.7f,
            inputSize = 640,
        )

        assertTrue(parsed.isEmpty())
    }

    @Test
    fun `the score comes from the class channel, not a box value`() {
        // Was "picks the highest scoring class per anchor", which the shipped
        // single-class export makes meaningless — there is no competition. What
        // still matters, and is easy to get wrong by an off-by-one, is that the
        // confidence is read from channel 4 and not from one of the four box
        // values sitting in front of it.
        val anchors = 1
        val output = FloatArray(channels * anchors)
        output[0] = 0.5f
        output[1] = 0.5f
        output[2] = 0.2f
        output[3] = 0.2f
        output[BOX_VALUES + 0] = 0.88f

        val parsed = WeaponOutputParser.parse(
            output = output,
            shape = intArrayOf(1, channels, anchors),
            confidenceThreshold = 0.5f,
            inputSize = 640,
        )

        assertEquals("Weapon", parsed.first().weaponType)
        assertEquals(0.88f, parsed.first().confidence, TOLERANCE)
    }

    @Test
    fun `normalises pixel-space coordinates`() {
        val anchors = 1
        val output = FloatArray(channels * anchors)
        // Same box as the normalised case, but expressed in 640-pixel space.
        output[0] = 320f
        output[1] = 320f
        output[2] = 256f
        output[3] = 256f
        output[BOX_VALUES + 0] = 0.9f

        val parsed = WeaponOutputParser.parse(
            output = output,
            shape = intArrayOf(1, channels, anchors),
            confidenceThreshold = 0.5f,
            inputSize = 640,
        )

        assertEquals(0.3f, parsed.first().boundingBox.left, TOLERANCE)
        assertEquals(0.7f, parsed.first().boundingBox.right, TOLERANCE)
    }

    @Test
    fun `refuses a tensor whose class count disagrees with the class map`() {
        // 80 COCO classes rather than this map's four. Parsing it would label a
        // person as a gun, so it yields nothing instead.
        val cocoChannels = BOX_VALUES + 80
        val output = FloatArray(cocoChannels * 10)

        val parsed = WeaponOutputParser.parse(
            output = output,
            shape = intArrayOf(1, cocoChannels, 10),
            confidenceThreshold = 0.5f,
            inputSize = 640,
        )

        assertTrue(parsed.isEmpty())
    }

    @Test
    fun `refuses a tensor of unexpected rank`() {
        val parsed = WeaponOutputParser.parse(
            output = FloatArray(100),
            shape = intArrayOf(1, 100),
            confidenceThreshold = 0.5f,
            inputSize = 640,
        )

        assertTrue(parsed.isEmpty())
    }

    @Test
    fun `clamps boxes that extend past the input edge`() {
        val anchors = 1
        val output = FloatArray(channels * anchors)
        output[0] = 0.05f
        output[1] = 0.05f
        output[2] = 0.4f
        output[3] = 0.4f
        output[BOX_VALUES + 0] = 0.9f

        val parsed = WeaponOutputParser.parse(
            output = output,
            shape = intArrayOf(1, channels, anchors),
            confidenceThreshold = 0.5f,
            inputSize = 640,
        )

        assertEquals(0f, parsed.first().boundingBox.left, TOLERANCE)
        assertEquals(0f, parsed.first().boundingBox.top, TOLERANCE)
    }

    private companion object {
        const val BOX_VALUES = 4
        const val TOLERANCE = 1e-4f
    }
}
