package com.securevision.feature.live

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securevision.core.common.Constants
import com.securevision.core.common.result.Result
import com.securevision.core.common.result.getOrDefault
import com.securevision.core.domain.engine.EnrolmentCapture
import com.securevision.core.domain.engine.FaceFrame
import com.securevision.core.domain.usecase.dashboard.GetEnrolledProfileCountUseCase
import com.securevision.core.domain.usecase.invoke
import com.securevision.core.domain.usecase.live.EnrolFaceFromFrameUseCase
import com.securevision.core.domain.usecase.live.EnrolmentException
import com.securevision.core.domain.usecase.live.PrepareRecognitionUseCase
import com.securevision.core.domain.usecase.live.RecogniseFacesUseCase
import com.securevision.core.domain.usecase.live.RecordUnknownSightingUseCase
import com.securevision.core.domain.usecase.profile.GetEnrolledProfilesUseCase
import com.securevision.core.domain.usecase.settings.ObserveSettingsUseCase
import com.securevision.core.model.AppSettings
import com.securevision.core.model.DetectionResult
import com.securevision.core.model.EnrolledProfile
import com.securevision.core.model.MatchStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Drives the live camera screen.
 *
 * Reaches the recognition pipeline only through domain use cases —
 * `feature-live` declares no dependency on `ml:ml-face`, so the detector,
 * aligner and TFLite are not on this module's compile classpath at all.
 */
@HiltViewModel
class LiveCameraViewModel @Inject constructor(
    private val prepareRecognition: PrepareRecognitionUseCase,
    private val recogniseFaces: RecogniseFacesUseCase,
    private val enrolFaceFromFrame: EnrolFaceFromFrameUseCase,
    private val recordUnknownSighting: RecordUnknownSightingUseCase,
    getEnrolledProfiles: GetEnrolledProfilesUseCase,
    getEnrolledProfileCount: GetEnrolledProfileCountUseCase,
    observeSettings: ObserveSettingsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<LiveUiState>(LiveUiState.Loading)

    /** Current screen state. */
    val uiState: StateFlow<LiveUiState> = _uiState.asStateFlow()

    private val _enrolmentEvent = MutableStateFlow<EnrolmentEvent?>(null)

    /** One-shot outcome of the most recent enrolment attempt. */
    val enrolmentEvent: StateFlow<EnrolmentEvent?> = _enrolmentEvent.asStateFlow()

    private var profiles: List<EnrolledProfile> = emptyList()
    private var settings: AppSettings = AppSettings()

    /** Most recent frame, retained so enrolment can capture what is on screen now. */
    private var latestFrame: FaceFrame? = null

    /**
     * Guards the pipeline against re-entry.
     *
     * CameraX delivers frames faster than a full detect–align–embed pass, and
     * without this the analyses would overlap and queue until the camera stalls.
     */
    private val isAnalysing = AtomicBoolean(false)
    private var lastAnalysedAt = 0L

    private val seenTrackingIds = mutableSetOf<Int>()
    private val knownTrackingIds = mutableSetOf<Int>()
    private val unknownTrackingIds = mutableSetOf<Int>()
    private val alertedTrackingIds = mutableSetOf<Int>()
    private var lastAlertAt = 0L

    init {
        _uiState.value = LiveUiState.Ready()

        viewModelScope.launch {
            observeSettings().collect { result ->
                settings = result.getOrDefault(AppSettings())
            }
        }

        viewModelScope.launch {
            getEnrolledProfiles(GetEnrolledProfilesUseCase.Params()).collect { result ->
                profiles = result.getOrDefault(emptyList())
                loadModelIfNeeded()
            }
        }

        viewModelScope.launch {
            getEnrolledProfileCount().collect { result ->
                val count = result.getOrDefault(0)
                _uiState.update { state ->
                    (state as? LiveUiState.Ready)?.copy(enrolledCount = count) ?: state
                }
            }
        }
    }

    /**
     * Feeds one analysed frame into the pipeline.
     *
     * Frames arriving while an analysis is running, or inside the throttle window,
     * are dropped rather than queued. A dropped frame costs nothing — the next one
     * is 33 ms away — whereas a queue would add latency that never recovers.
     *
     * @param bitmap Upright frame.
     * @param isFrontCamera Which camera produced it.
     */
    fun onFrame(bitmap: Bitmap, isFrontCamera: Boolean) {
        val now = System.currentTimeMillis()
        if (now - lastAnalysedAt < ANALYSIS_INTERVAL_MILLIS) return
        if (!isAnalysing.compareAndSet(false, true)) return

        lastAnalysedAt = now

        val frame = FaceFrame(
            bitmap = bitmap,
            isFrontCamera = isFrontCamera,
            timestampMillis = now,
        )
        latestFrame = frame

        viewModelScope.launch {
            try {
                val result = recogniseFaces(
                    RecogniseFacesUseCase.Params(
                        frame = frame,
                        profiles = profiles,
                        settings = settings,
                    ),
                )

                val detections = when (result) {
                    is Result.Success -> result.data
                    else -> emptyList()
                }

                updateStats(detections)
                recordConfirmedUnknowns(detections, isFrontCamera, now)

                _uiState.update { state ->
                    (state as? LiveUiState.Ready)?.copy(
                        detections = detections,
                        stats = currentStats(),
                        analysisWidth = bitmap.width,
                        analysisHeight = bitmap.height,
                        isFrontCamera = isFrontCamera,
                    ) ?: state
                }
            } finally {
                isAnalysing.set(false)
            }
        }
    }

    /**
     * Enrols the face currently in frame.
     *
     * @param name Display name for the new profile.
     * @param age Age in years.
     */
    fun enrolCurrentFace(name: String, age: Int) {
        val frame = latestFrame ?: run {
            _enrolmentEvent.value = EnrolmentEvent.Failed(
                EnrolmentCapture.Failure.Reason.NO_FACE_DETECTED,
            )
            return
        }

        _uiState.update { state -> (state as? LiveUiState.Ready)?.copy(isEnrolling = true) ?: state }

        viewModelScope.launch {
            val result = enrolFaceFromFrame(
                EnrolFaceFromFrameUseCase.Params(frame = frame, name = name, age = age),
            )

            _enrolmentEvent.value = when (result) {
                is Result.Success -> EnrolmentEvent.Enrolled(result.data.name)
                is Result.Error -> EnrolmentEvent.Failed(
                    (result.throwable as? EnrolmentException)?.reason,
                )
                Result.Loading -> null
            }

            _uiState.update { state ->
                (state as? LiveUiState.Ready)?.copy(isEnrolling = false) ?: state
            }
        }
    }

    /** Clears the last enrolment outcome once it has been shown. */
    fun consumeEnrolmentEvent() {
        _enrolmentEvent.value = null
    }

    /**
     * Switches between front and back cameras.
     *
     * Session counters and voting history are both reset: tracking ids restart on
     * the new camera, so carrying either across would double-count the same person
     * and let stale votes decide a new face.
     */
    fun flipCamera() {
        seenTrackingIds.clear()
        knownTrackingIds.clear()
        unknownTrackingIds.clear()
        alertedTrackingIds.clear()

        _uiState.update { state ->
            (state as? LiveUiState.Ready)?.copy(
                isFrontCamera = !state.isFrontCamera,
                detections = emptyList(),
                stats = SessionStats(),
            ) ?: state
        }
    }

    private suspend fun loadModelIfNeeded() {
        val currentStatus = (_uiState.value as? LiveUiState.Ready)?.engineStatus
        if (currentStatus is com.securevision.core.domain.engine.EngineStatus.Ready) return

        val status = prepareRecognition(
            PrepareRecognitionUseCase.Params(
                enrolledDimensions = profiles.firstOrNull()?.embeddingSize,
            ),
        )

        val resolved = (status as? Result.Success)?.data ?: return

        _uiState.update { state ->
            (state as? LiveUiState.Ready)?.copy(engineStatus = resolved) ?: state
        }
    }

    private fun updateStats(detections: List<DetectionResult>) {
        detections.forEach { detection ->
            if (detection.trackingId < 0) return@forEach

            seenTrackingIds += detection.trackingId

            when (detection.matchStatus) {
                MatchStatus.KNOWN -> {
                    knownTrackingIds += detection.trackingId
                    unknownTrackingIds -= detection.trackingId
                }
                MatchStatus.UNKNOWN -> if (detection.trackingId !in knownTrackingIds) {
                    unknownTrackingIds += detection.trackingId
                }
                MatchStatus.PROCESSING -> Unit
            }
        }
    }

    private fun currentStats() = SessionStats(
        total = seenTrackingIds.size,
        known = knownTrackingIds.size,
        unknown = unknownTrackingIds.size,
    )

    /**
     * Writes an alert for each newly confirmed stranger.
     *
     * Guarded twice: once per tracking id, so a face held in view for ten seconds
     * produces one alert rather than thirty, and once by a time window, so a
     * detector that keeps reassigning ids cannot bypass the first guard.
     */
    private fun recordConfirmedUnknowns(
        detections: List<DetectionResult>,
        isFrontCamera: Boolean,
        now: Long,
    ) {
        detections
            .filter { it.matchStatus == MatchStatus.UNKNOWN && it.trackingId >= 0 }
            .filter { it.trackingId !in alertedTrackingIds }
            .forEach { detection ->
                if (now - lastAlertAt < Constants.Alerting.DUPLICATE_ALERT_WINDOW_MILLIS) return

                alertedTrackingIds += detection.trackingId
                lastAlertAt = now

                viewModelScope.launch {
                    recordUnknownSighting(
                        RecordUnknownSightingUseCase.Params(
                            confidence = detection.confidence,
                            cameraFacing = if (isFrontCamera) FRONT else BACK,
                            timestamp = now,
                        ),
                    )
                }
            }
    }

    private companion object {
        /**
         * Minimum gap between analyses.
         *
         * A full pass is detect plus, per face, align and a model inference. Below
         * roughly this interval the pipeline cannot keep up on a mid-range device
         * and the preview starts to stutter.
         */
        const val ANALYSIS_INTERVAL_MILLIS = 350L

        const val FRONT = "front"
        const val BACK = "back"
    }
}

/** One-shot outcome of an enrolment attempt. */
sealed interface EnrolmentEvent {

    /**
     * A profile was created.
     *
     * @property name The enrolled person's name.
     */
    data class Enrolled(val name: String) : EnrolmentEvent

    /**
     * Enrolment failed.
     *
     * @property reason Why, so the screen can say what to change. `null` when the
     *   failure was not one the engine recognised.
     */
    data class Failed(val reason: EnrolmentCapture.Failure.Reason?) : EnrolmentEvent
}
