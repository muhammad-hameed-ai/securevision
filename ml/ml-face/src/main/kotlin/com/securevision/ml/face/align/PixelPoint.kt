package com.securevision.ml.face.align

/**
 * A point in pixel coordinates within some bitmap.
 *
 * Distinct from [com.securevision.core.model.NormalisedPoint], which is in
 * `0f..1f` frame space. Alignment works in pixels because the transform it solves
 * has to land on the template's absolute positions inside a 160×160 crop, and
 * mixing the two spaces is exactly the kind of error that silently produces a
 * warp that looks plausible and embeds badly.
 *
 * @property x Horizontal pixel position.
 * @property y Vertical pixel position.
 */
data class PixelPoint(val x: Float, val y: Float)
