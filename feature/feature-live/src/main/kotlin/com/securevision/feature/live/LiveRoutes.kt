package com.securevision.feature.live

/**
 * Navigation identity of the live camera feature.
 *
 * Phase 4 adds `LiveScreen`, `LiveViewModel` and the overlay renderer alongside
 * this file: the CameraX preview, the detection overlay that draws green boxes
 * for known people, red for unknown and orange for weapons, and the alarm and
 * recording controls.
 */
object LiveRoutes {

    /** Real-time camera and detection destination. */
    const val LIVE = "live"
}
