package com.securevision.feature.live

import androidx.compose.runtime.Immutable
import com.securevision.core.domain.engine.EngineStatus
import com.securevision.core.model.DetectionResult

/**
 * Everything the live screen renders.
 *
 * [Ready] deliberately carries [engineStatus] rather than a plain boolean: the
 * screen has to distinguish "no model installed" from "the model emits a
 * different dimension than your enrolments" — the same red banner for both would
 * hide the fact that one is fixed by adding a file and the other by re-enrolling.
 */
@Immutable
sealed interface LiveUiState {

    /** Camera and model are still starting. */
    data object Loading : LiveUiState

    /**
     * The camera is running.
     *
     * @property detections Faces in the most recent analysed frame.
     * @property stats Counts for this session.
     * @property engineStatus Whether recognition is available, and on what.
     * @property analysisWidth Width of the upright analysis frame, for the overlay.
     * @property analysisHeight Height of the upright analysis frame, for the overlay.
     * @property isFrontCamera Which camera is active; the overlay mirrors for it.
     * @property enrolledCount How many people the app can currently recognise.
     * @property isEnrolling Whether an enrolment capture is in flight.
     */
    @Immutable
    data class Ready(
        val detections: List<DetectionResult> = emptyList(),
        val stats: SessionStats = SessionStats(),
        val engineStatus: EngineStatus = EngineStatus.Initialising,
        val analysisWidth: Int = 0,
        val analysisHeight: Int = 0,
        val isFrontCamera: Boolean = false,
        val enrolledCount: Int = 0,
        val isEnrolling: Boolean = false,
    ) : LiveUiState {

        /** Whether faces can actually be recognised, as opposed to merely detected. */
        val isRecognitionActive: Boolean get() = engineStatus is EngineStatus.Ready

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
 * Counts for the current session, reset when the screen is left.
 *
 * Counted per tracking id rather than per frame: one person standing in view for
 * a minute is one sighting, not two hundred.
 *
 * @property total Distinct faces seen.
 * @property known Distinct faces recognised as an enrolled person.
 * @property unknown Distinct faces confirmed as matching nobody.
 */
@Immutable
data class SessionStats(
    val total: Int = 0,
    val known: Int = 0,
    val unknown: Int = 0,
)
