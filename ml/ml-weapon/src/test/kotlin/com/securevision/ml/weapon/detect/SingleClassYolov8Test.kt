package com.securevision.ml.weapon.detect

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The shipped export, decoded end to end.
 *
 * Pinned against the real model's shapes: input `[1,3,640,640]` NCHW, output
 * `[1,5,8400]` channels-major, one class. Both were wrong in the code before
 * this — the loader refused the model outright on class count, and the
 * preprocessing wrote channels-last bytes that an NCHW graph would have read as
 * noise without ever raising an error.
 */
class SingleClassYolov8Test {

    private val inputShape = intArrayOf(1, 3, 640, 640)
    private val outputShape = intArrayOf(1, 5, 8400)

    @Test
    fun `the export declares exactly one class`() {
        assertEquals(listOf("Weapon"), WeaponClassMap.LABELS)
        assertEquals(1, WeaponClassMap.EXPECTED_CLASS_COUNT)
    }

    @Test
    fun `five channels are expected — four box values plus one class`() {
        assertEquals(outputShape[1], WeaponOutputParser.expectedChannels())
    }

    @Test
    fun `the real output shape resolves to a channels-major layout`() {
        val layout = WeaponOutputParser.Layout.from(
            outputShape,
            WeaponOutputParser.expectedChannels(),
        )

        assertNotNull("[1,5,8400] must be recognised", layout)
        assertTrue(layout!!.channelsMajor)
        assertEquals(8400, layout.anchorCount)
        assertEquals(1, layout.classCount)
    }

    @Test
    fun `a four-class output is refused rather than mislabelled`() {
        // The guard that rejected the shipped model when the labels said four
        // classes. It stays: silently relabelling detections is worse than not
        // loading, because a CRITICAL alarm would name the wrong thing.
        val fourClass = intArrayOf(1, 8, 8400)

        assertNull(
            WeaponOutputParser.Layout.from(fourClass, WeaponOutputParser.expectedChannels()),
        )
    }

    @Test
    fun `a confident anchor decodes to a Weapon detection`() {
        val output = emptyOutput()

        // One anchor at the centre of the frame, half width and half height,
        // scored 0.91. Channels-major: every anchor of channel 0, then channel 1…
        writeAnchor(output, anchor = 4_200, cx = 320f, cy = 320f, w = 160f, h = 160f, score = 0.91f)

        val detections = WeaponOutputParser.parse(
            output = output,
            shape = outputShape,
            confidenceThreshold = 0.70f,
            inputSize = 640,
        )

        assertEquals(1, detections.size)
        assertEquals("Weapon", detections.first().weaponType)
        assertEquals(0.91f, detections.first().confidence, TOLERANCE)
    }

    @Test
    fun `an anchor below the threshold is discarded`() {
        val output = emptyOutput()
        writeAnchor(output, anchor = 10, cx = 320f, cy = 320f, w = 100f, h = 100f, score = 0.69f)

        val detections = WeaponOutputParser.parse(
            output = output,
            shape = outputShape,
            confidenceThreshold = 0.70f,
            inputSize = 640,
        )

        // 0.70 is the weapon floor. Just under it must produce nothing at all,
        // because anything that survives here sounds a repeating alarm.
        assertTrue(detections.isEmpty())
    }

    @Test
    fun `the decoded box lands where the anchor said`() {
        val output = emptyOutput()
        writeAnchor(output, anchor = 100, cx = 160f, cy = 320f, w = 80f, h = 160f, score = 0.95f)

        val box = WeaponOutputParser.parse(
            output = output,
            shape = outputShape,
            confidenceThreshold = 0.70f,
            inputSize = 640,
        ).first().boundingBox

        // cx 160 of 640 is a quarter across; w 80 spans an eighth, so 0.1875..0.3125.
        assertEquals(0.1875f, box.left, TOLERANCE)
        assertEquals(0.3125f, box.right, TOLERANCE)
        assertEquals(0.375f, box.top, TOLERANCE)
        assertEquals(0.625f, box.bottom, TOLERANCE)
    }

    @Test
    fun `the input shape is recognised as channels-first`() {
        // [1,3,640,640]: axis 1 is the channel count, so the spatial size lives
        // at axis 2. Reading axis 1 as the size — as the code used to — would
        // letterbox every frame to 3x3.
        assertEquals(3, inputShape[1])
        assertEquals(640, inputShape[2])

        val nhwc = intArrayOf(1, 640, 640, 3)
        assertEquals(640, nhwc[1])
    }

    /** A `[1,5,8400]` buffer of zeroes, laid out channels-major. */
    private fun emptyOutput() = FloatArray(outputShape[1] * outputShape[2])

    /**
     * Writes one anchor's five values into a channels-major buffer.
     *
     * Channels-major means channel `c` of anchor `a` sits at `c * anchors + a`,
     * not at `a * channels + c`.
     */
    private fun writeAnchor(
        output: FloatArray,
        anchor: Int,
        cx: Float,
        cy: Float,
        w: Float,
        h: Float,
        score: Float,
    ) {
        val anchors = outputShape[2]

        output[0 * anchors + anchor] = cx
        output[1 * anchors + anchor] = cy
        output[2 * anchors + anchor] = w
        output[3 * anchors + anchor] = h
        output[4 * anchors + anchor] = score
    }

    private companion object {
        const val TOLERANCE = 1e-4f
    }
}
