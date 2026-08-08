package com.securevision.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Detection colours that Material 3's `ColorScheme` has no slot for.
 *
 * `ColorScheme` models primary/secondary/error, not "recognised person" or
 * "weapon". Rather than abusing `tertiary` to mean "known face", these live in
 * their own palette exposed through `SecureVisionTheme.colors`.
 *
 * The solid [known], [unknown], [weapon] and [motion] values are identical in
 * both themes: they are stroked over the camera image, which has no theme. Only
 * the `…Container` and `on…` pairings — used for badges sitting on an app
 * surface — differ between light and dark.
 *
 * @property known Overlay colour for a recognised, enrolled person.
 * @property onKnown Readable content colour on top of [known].
 * @property knownContainer Tinted badge background for a known-person alert.
 * @property onKnownContainer Readable content colour on [knownContainer].
 * @property unknown Overlay colour for an unrecognised face.
 * @property onUnknown Readable content colour on top of [unknown].
 * @property unknownContainer Tinted badge background for an unknown-person alert.
 * @property onUnknownContainer Readable content colour on [unknownContainer].
 * @property weapon Overlay colour for a detected weapon.
 * @property onWeapon Readable content colour on top of [weapon].
 * @property weaponContainer Tinted badge background for a weapon alert.
 * @property onWeaponContainer Readable content colour on [weaponContainer].
 * @property motion Overlay colour for a motion event.
 * @property onMotion Readable content colour on top of [motion].
 * @property motionContainer Tinted badge background for a motion alert.
 * @property onMotionContainer Readable content colour on [motionContainer].
 * @property cameraScrim Translucent wash behind controls laid over the preview.
 */
@Immutable
data class SecureVisionColors(
    val known: Color,
    val onKnown: Color,
    val knownContainer: Color,
    val onKnownContainer: Color,
    val unknown: Color,
    val onUnknown: Color,
    val unknownContainer: Color,
    val onUnknownContainer: Color,
    val weapon: Color,
    val onWeapon: Color,
    val weaponContainer: Color,
    val onWeaponContainer: Color,
    val motion: Color,
    val onMotion: Color,
    val motionContainer: Color,
    val onMotionContainer: Color,
    val cameraScrim: Color,
)

/** Detection palette for the dark theme. */
internal val SecureVisionDarkDetectionColors = SecureVisionColors(
    known = DetectionKnownGreen,
    onKnown = Color(0xFF00281A),
    knownContainer = Color(0xFF0C3B2A),
    onKnownContainer = Color(0xFF7BF3BE),

    unknown = DetectionUnknownRed,
    onUnknown = Color(0xFFFFFFFF),
    unknownContainer = Color(0xFF4A1512),
    onUnknownContainer = Color(0xFFFFB4AE),

    weapon = DetectionWeaponOrange,
    onWeapon = Color(0xFF241000),
    weaponContainer = Color(0xFF452200),
    onWeaponContainer = Color(0xFFFFB786),

    motion = DetectionMotionAmber,
    onMotion = Color(0xFF2A2000),
    motionContainer = Color(0xFF3D3000),
    onMotionContainer = Color(0xFFFFE08A),

    cameraScrim = Color(0xB30A1628),
)

/** Detection palette for the light theme. */
internal val SecureVisionLightDetectionColors = SecureVisionColors(
    known = DetectionKnownGreen,
    onKnown = Color(0xFF00281A),
    knownContainer = Color(0xFFC5F5DF),
    onKnownContainer = Color(0xFF003823),

    unknown = DetectionUnknownRed,
    onUnknown = Color(0xFFFFFFFF),
    unknownContainer = Color(0xFFFFDAD6),
    onUnknownContainer = Color(0xFF5E0F0A),

    weapon = DetectionWeaponOrange,
    onWeapon = Color(0xFF241000),
    weaponContainer = Color(0xFFFFDCC6),
    onWeaponContainer = Color(0xFF562A00),

    motion = DetectionMotionAmber,
    onMotion = Color(0xFF2A2000),
    motionContainer = Color(0xFFFFEFC2),
    onMotionContainer = Color(0xFF4A3A00),

    cameraScrim = Color(0x99000000),
)
