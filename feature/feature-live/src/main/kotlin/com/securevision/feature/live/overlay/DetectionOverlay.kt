package com.securevision.feature.live.overlay

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.securevision.core.model.DetectionResult
import com.securevision.core.model.MatchStatus
import com.securevision.core.model.WeaponDetection
import com.securevision.core.ui.theme.SecureVisionTheme
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Draws a box and label over every detected face.
 *
 * Colour carries meaning: green for a recognised person, red for a confirmed
 * stranger, cyan while voting is still undecided. The third state is not
 * decoration — showing red before voting has committed would flash "UNKNOWN" over
 * someone the app is about to recognise correctly.
 *
 * @param detections Faces in the most recent analysed frame.
 * @param transform Maps normalised boxes onto this surface.
 * @param modifier Modifier applied to the canvas.
 */
@Composable
fun DetectionOverlay(
    detections: List<DetectionResult>,
    weapons: List<WeaponDetection>,
    transform: OverlayTransform,
    modifier: Modifier = Modifier,
) {
    val palette = SecureVisionTheme.colors
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    val strokeWidth = with(density) { STROKE_WIDTH.toPx() }
    val weaponStrokeWidth = with(density) { WEAPON_STROKE_WIDTH.toPx() }
    val cornerLength = with(density) { CORNER_LENGTH.toPx() }
    val labelOffset = with(density) { LABEL_OFFSET.toPx() }

    Canvas(modifier = modifier.fillMaxSize()) {
        detections.forEach { detection ->
            val rect = transform.project(detection.boundingBox)

            val colour = when (detection.displayStatus()) {
                MatchStatus.KNOWN -> palette.known
                MatchStatus.UNKNOWN -> palette.unknown
                MatchStatus.PROCESSING -> Color(PROCESSING_COLOUR)
            }

            drawCornerBrackets(
                rect = rect,
                colour = colour,
                strokeWidth = strokeWidth,
                cornerLength = cornerLength,
            )

            drawLabel(
                text = detection.label(),
                rect = rect,
                colour = colour,
                textMeasurer = textMeasurer,
                labelOffset = labelOffset,
            )
        }

        // Weapons are drawn last so they sit above any face box they overlap, and
        // with a heavier stroke: if both are on screen, the weapon is the thing
        // the operator must see first.
        weapons.forEach { weapon ->
            val rect = transform.project(weapon.boundingBox)

            drawCornerBrackets(
                rect = rect,
                colour = palette.weapon,
                strokeWidth = weaponStrokeWidth,
                cornerLength = cornerLength,
            )

            drawLabel(
                text = weapon.label(),
                rect = rect,
                colour = palette.weapon,
                textMeasurer = textMeasurer,
                labelOffset = labelOffset,
            )
        }
    }
}

/**
 * The status this face is actually **drawn** as.
 *
 * A security control, not a formatting helper. Green claims "I recognise this
 * person", so it is only ever painted when there is a person to name: a KNOWN
 * verdict carrying no `profileId` or no name is a contradiction, and it is
 * downgraded rather than trusted.
 *
 * Before this, a face the pipeline had not resolved was painted `#00C9A7` —
 * visually identical to the `#00D97E` used for a genuine match on a moving
 * preview — so anything that failed silently looked like a successful
 * recognition. That is a false positive in a security product, and the fix is to
 * make it unrepresentable rather than unlikely.
 */
internal fun DetectionResult.displayStatus(): MatchStatus = when (matchStatus) {
    MatchStatus.KNOWN -> if (hasResolvedIdentity()) MatchStatus.KNOWN else MatchStatus.PROCESSING
    else -> matchStatus
}

/** Whether a KNOWN verdict can actually name the person it claims to recognise. */
internal fun DetectionResult.hasResolvedIdentity(): Boolean =
    !profileId.isNullOrBlank() && !profileName.isNullOrBlank()

private fun DetectionResult.label(): String = when (displayStatus()) {
    MatchStatus.KNOWN -> "${profileName.orEmpty()}  ${(confidence * PERCENT).roundToInt()}%"
    MatchStatus.UNKNOWN -> UNKNOWN_LABEL
    MatchStatus.PROCESSING -> PROCESSING_LABEL
}

private fun WeaponDetection.label(): String =
    "${weaponType.uppercase()}  ${(confidence * PERCENT).roundToInt()}%"

/**
 * Draws four corner brackets rather than a closed rectangle.
 *
 * A full outline hides the edges of the face it is meant to highlight; brackets
 * mark the same bounds while leaving the subject visible, which matters when the
 * operator is trying to judge whether the box is actually on the face.
 */
private fun DrawScope.drawCornerBrackets(
    rect: ViewRect,
    colour: Color,
    strokeWidth: Float,
    cornerLength: Float,
) {
    // Never let a bracket exceed a third of the box, or a small face becomes a
    // solid rectangle again.
    val length = min(cornerLength, min(rect.width, rect.height) / 3f)
    if (length <= 0f) return

    val stroke = Stroke(width = strokeWidth)

    // Top-left
    drawLine(colour, Offset(rect.left, rect.top), Offset(rect.left + length, rect.top), strokeWidth)
    drawLine(colour, Offset(rect.left, rect.top), Offset(rect.left, rect.top + length), strokeWidth)
    // Top-right
    drawLine(colour, Offset(rect.right - length, rect.top), Offset(rect.right, rect.top), strokeWidth)
    drawLine(colour, Offset(rect.right, rect.top), Offset(rect.right, rect.top + length), strokeWidth)
    // Bottom-left
    drawLine(colour, Offset(rect.left, rect.bottom - length), Offset(rect.left, rect.bottom), strokeWidth)
    drawLine(colour, Offset(rect.left, rect.bottom), Offset(rect.left + length, rect.bottom), strokeWidth)
    // Bottom-right
    drawLine(colour, Offset(rect.right, rect.bottom - length), Offset(rect.right, rect.bottom), strokeWidth)
    drawLine(colour, Offset(rect.right - length, rect.bottom), Offset(rect.right, rect.bottom), strokeWidth)

    // A faint full outline underneath keeps the bounds readable against a busy
    // background without obscuring the face.
    drawRect(
        color = colour.copy(alpha = OUTLINE_ALPHA),
        topLeft = Offset(rect.left, rect.top),
        size = Size(rect.width, rect.height),
        style = stroke,
    )
}

private fun DrawScope.drawLabel(
    text: String,
    rect: ViewRect,
    colour: Color,
    textMeasurer: TextMeasurer,
    labelOffset: Float,
) {
    val measured = textMeasurer.measure(
        text = text,
        style = TextStyle(color = colour, fontSize = LABEL_SIZE),
    )

    // Above the box normally, inside it when the face is near the top edge and
    // the label would otherwise be clipped off-screen.
    val labelTop = if (rect.top - measured.size.height - labelOffset > 0f) {
        rect.top - measured.size.height - labelOffset
    } else {
        rect.top + labelOffset
    }

    drawRect(
        color = Color.Black.copy(alpha = LABEL_BACKDROP_ALPHA),
        topLeft = Offset(rect.left, labelTop),
        size = Size(measured.size.width.toFloat(), measured.size.height.toFloat()),
    )

    drawText(
        textLayoutResult = measured,
        topLeft = Offset(rect.left, labelTop),
    )
}

private val STROKE_WIDTH = 3.dp

/** Heavier than a face box, so a weapon reads first when both are on screen. */
private val WEAPON_STROKE_WIDTH = 5.dp

private val CORNER_LENGTH = 22.dp
private val LABEL_OFFSET = 6.dp
private val LABEL_SIZE = 14.sp

private const val PERCENT = 100f
private const val OUTLINE_ALPHA = 0.35f
private const val LABEL_BACKDROP_ALPHA = 0.55f

/**
 * Amber for a face the pipeline has not resolved.
 *
 * Deliberately **not** the brand cyan this used to be. `#00C9A7` sat a few
 * degrees of hue from the `#00D97E` used for a recognised person and read as
 * green on a live preview — so an unresolved face looked recognised. Amber
 * cannot be confused with either green or the red used for a stranger.
 */
private const val PROCESSING_COLOUR = 0xFFFFB300

private const val UNKNOWN_LABEL = "UNKNOWN"
private const val PROCESSING_LABEL = "…"
