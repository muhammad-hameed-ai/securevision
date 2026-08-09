package com.securevision.feature.live

import androidx.compose.runtime.Immutable
import com.securevision.core.domain.engine.EngineStatus
import com.securevision.core.model.DetectionResult
import com.securevision.core.model.MotionResult
import com.securevision.core.model.WeaponDetection

/**
 * Everything the live screen renders.
 *
 * Engine statuses are carried separately per detector, because they fail
 * independently and their fixes differ: a missing face model needs a file, a
 * dimension mismatch needs re-enrolment, and a missing weapon model needs a
 * different file entirely. One combined "degraded" flag would hide which.
 */
@Immutable
sealed interface LiveUiState {

    /** Camera and models are still starting. */
    data object Loading : LiveUiState

    /**
     * The camera is running.
     *
     * @property detections Faces in the most recent analysed frame.
     * @property weapons Weapons in the most recent frame that ran weapon detection.
     * @property motion Latest frame-to-frame comparison.
     * @property stats Counts for this session.
     * @property faceEngineStatus Whether face recognition is available.
     * @property weaponEngineStatus Whether weapon detection is available.
     * @property attributesEnabled Whether attribute analysis is switched on.
     * @property isAlarmSounding Whether a repeating critical alarm is playing, in
     *   which case the screen offers a Silence control.
     * @property notificationsBlocked Whether the last alert could not reach the
     *   notification shade because the system permission is refused. Shown as an
     *   explanation, never as a blocker — the alert itself was still recorded.
     * @property analysisWidth Width of the upright analysis frame, for the overlay.
     * @property analysisHeight Height of the upright analysis frame, for the overlay.
     * @property isFrontCamera Which camera is active; the overlay mirrors for it.
     * @property enrolledCount How many people the app can currently recognise.
     * @property isEnrolling Whether an enrolment capture is in flight.
     */
    @Immutable
    data class Ready(
        val detections: List<DetectionResult> = emptyList(),
        val weapons: List<WeaponDetection> = emptyList(),
        val motion: MotionResult = MotionResult.NONE,
        val stats: SessionStats = SessionStats(),
        val faceEngineStatus: EngineStatus = EngineStatus.Initialising,
        val weaponEngineStatus: EngineStatus = EngineStatus.Initialising,
        val attributesEnabled: Boolean = false,
        val isAlarmSounding: Boolean = false,
        val notificationsBlocked: Boolean = false,
        val analysisWidth: Int = 0,
        val analysisHeight: Int = 0,
        val isFrontCamera: Boolean = false,
        val enrolledCount: Int = 0,
        val isEnrolling: Boolean = false,
    ) : LiveUiState {

        /** Whether faces can be recognised, as opposed to merely detected. */
        val isRecognitionActive: Boolean get() = faceEngineStatus is EngineStatus.Ready

        /** Whether weapons can be detected at all. */
        val isWeaponDetectionActive: Boolean get() = weaponEngineStatus is EngineStatus.Ready

        /** Whether the overlay has enough frame geometry to project boxes. */
        val canProjectOverlay: Boolean get() = analysisWidth > 0 && analysisHeight > 0
    }

    /**
     * The camera could not be started.
     *
     * @property message Cause, when one was reported.
     */
    data class Error(val message: String?) : LiveUiState
}

/**
 * Counts for the current session, reset when the camera flips or the screen is left.
 *
 * Faces are counted per tracking id rather than per frame: one person standing in
 * view for a minute is one sighting, not two hundred. Weapons have no tracking id,
 * so they are counted per confirmed alert, which the de-duplication guard already
 * bounds.
 *
 * @property total Distinct faces seen.
 * @property known Distinct faces recognised as an enrolled person.
 * @property unknown Distinct faces confirmed as matching nobody.
 * @property weapons Weapon alerts raised.
 */
@Immutable
data class SessionStats(
    val total: Int = 0,
    val known: Int = 0,
    val unknown: Int = 0,
    val weapons: Int = 0,
)
