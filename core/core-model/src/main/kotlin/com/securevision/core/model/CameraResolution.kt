package com.securevision.core.model

/**
 * Capture resolution for the live camera.
 *
 * Resolution is a direct trade against inference latency: every extra pixel is
 * work the detector must do per frame, so a lower setting is the first lever to
 * reach for on a device that cannot hold a smooth preview.
 *
 * @property width Frame width in pixels.
 * @property height Frame height in pixels.
 */
enum class CameraResolution(val width: Int, val height: Int) {

    /** 640×480 — lowest latency, for older or thermally limited devices. */
    SD_480(width = 640, height = 480),

    /** 1280×720 — the default; the best balance of detail against frame time. */
    HD_720(width = 1280, height = 720),

    /** 1920×1080 — most detail, highest per-frame cost. */
    FHD_1080(width = 1920, height = 1080);

    /** Total pixels per frame, the figure that actually drives inference cost. */
    val pixelCount: Int get() = width * height
}
