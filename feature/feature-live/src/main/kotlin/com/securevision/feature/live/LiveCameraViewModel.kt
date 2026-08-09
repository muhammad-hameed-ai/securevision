package com.securevision.feature.live

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securevision.core.common.Constants
import com.securevision.core.common.result.Result
import com.securevision.core.common.result.getOrDefault
import com.securevision.core.domain.engine.EngineStatus
import com.securevision.core.domain.engine.EnrolmentCapture
import com.securevision.core.domain.engine.FaceFrame
import com.securevision.core.domain.engine.RecognisedFace
import com.securevision.core.domain.usecase.dashboard.GetEnrolledProfileCountUseCase
import com.securevision.core.domain.usecase.invoke
import com.securevision.core.domain.usecase.live.AnalyseAttributesUseCase
import com.securevision.core.domain.usecase.live.CaptureSnapshotUseCase
import com.securevision.core.domain.usecase.live.DetectMotionUseCase
import com.securevision.core.domain.usecase.live.DetectWeaponsUseCase
import com.securevision.core.domain.usecase.live.EnrolFaceFromFrameUseCase
import com.securevision.core.domain.usecase.live.EnrolmentException
import com.securevision.core.domain.usecase.live.PrepareDetectorsUseCase
import com.securevision.core.domain.usecase.live.RecogniseFacesUseCase
import com.securevision.core.domain.usecase.live.RecordMotionSightingUseCase
import com.securevision.core.domain.usecase.live.RecordUnknownSightingUseCase
import com.securevision.core.domain.usecase.live.RecordWeaponSightingUseCase
import com.securevision.core.domain.usecase.profile.GetEnrolledProfilesUseCase
import com.securevision.core.domain.usecase.settings.ObserveSettingsUseCase
import com.securevision.core.model.AppSettings
import com.securevision.core.model.EnrolledProfile
import com.securevision.core.model.FaceAttributes
import com.securevision.core.model.MatchStatus
import com.securevision.core.model.MotionResult
import com.securevision.core.model.WeaponDetection
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Drives the live camera screen and its three detectors.
 *
 * Reaches every pipeline only through domain use cases — `feature-live` declares
 * no dependency on `ml-face`, `ml-weapon`, `ml-motion` or `ml-attributes`, so none
 * of their types are even on this module's compile classpath.
 */
@HiltViewModel
class LiveCameraViewModel @Inject constructor(
    private val prepareDetectors: PrepareDetectorsUseCase,
    private val recogniseFaces: RecogniseFacesUseCase,
    private val detectWeapons: DetectWeaponsUseCase,
    private val detectMotion: DetectMotionUseCase,
    private val analyseAttributes: AnalyseAttributesUseCase,
    private val captureSnapshot: CaptureSnapshotUseCase,
    private val enrolFaceFromFrame: EnrolFaceFromFrameUseCase,
    private val recordUnknownSighting: RecordUnknownSightingUseCase,
    private val recordWeaponSighting: RecordWeaponSightingUseCase,
    private val recordMotionSighting: RecordMotionSightingUseCase,
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
     * without this the analyses overlap and queue until the camera stalls.
     */
    private val isAnalysing = AtomicBoolean(false)
    private var lastAnalysedAt = 0L
    private var cycle = 0L

    private val seenTrackingIds = mutableSetOf<Int>()
    private val knownTrackingIds = mutableSetOf<Int>()
    private val unknownTrackingIds = mutableSetOf<Int>()
    private var weaponAlertCount = 0

    /**
     * The single de-duplication guard, shared by every alert type.
     *
     * Keyed `kind:id` — tracking id for faces, class name for weapons, which have
     * no tracking id. One guard rather than one per detector, so the alarms and
     * notifications of Phase 5b inherit exactly this suppression.
     */
    private val alertedKeys = mutableSetOf<String>()

    /**
     * When each *kind* of alert last fired.
     *
     * Per kind, not global. A single timestamp across all kinds would let a face
     * alert swallow a weapon alert that follows it three seconds later — the
     * highest-severity event in the app silently dropped because of an unrelated
     * one. This matches what the window is documented to mean: minimum spacing
     * between repeated alerts *of the same kind*.
     */
    private val lastAlertAtByKind = mutableMapOf<String, Long>()

    init {
        _uiState.value = LiveUiState.Ready()

        viewModelScope.launch {
            observeSettings().collect { result ->
                settings = result.getOrDefault(AppSettings())
                _uiState.update { state ->
                    (state as? LiveUiState.Ready)
                        ?.copy(attributesEnabled = settings.attributeAnalysisEnabled)
                        ?: state
                }
            }
        }

        viewModelScope.launch {
            getEnrolledProfiles(GetEnrolledProfilesUseCase.Params()).collect { result ->
                profiles = result.getOrDefault(emptyList())
                loadModelsIfNeeded()
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
     * Feeds one analysed frame through every detector.
     *
     * Frames arriving while an analysis is running, or inside the throttle window,
     * are dropped rather than queued: the next one is milliseconds away, whereas a
     * queue adds latency that never recovers.
     *
     * @param bitmap Upright frame.
     * @param isFrontCamera Which camera produced it.
     */
    fun onFrame(bitmap: Bitmap, isFrontCamera: Boolean) {
        val now = System.currentTimeMillis()
        if (now - lastAnalysedAt < ANALYSIS_INTERVAL_MILLIS) return
        if (!isAnalysing.compareAndSet(false, true)) return

        lastAnalysedAt = now

        // Post-increment, so the first analysed frame is cycle 0 and therefore runs
        // weapon detection immediately rather than a throttle window later.
        val analysisCycle = cycle++

        val frame = FaceFrame(bitmap = bitmap, isFrontCamera = isFrontCamera, timestampMillis = now)
        latestFrame = frame

        viewModelScope.launch {
            try {
                analyse(frame, bitmap, isFrontCamera, now, analysisCycle)
            } finally {
                isAnalysing.set(false)
            }
        }
    }

    private suspend fun analyse(
        frame: FaceFrame,
        bitmap: Bitmap,
        isFrontCamera: Boolean,
        now: Long,
        analysisCycle: Long,
    ) = coroutineScope {
        val wantsAttributes = settings.attributeAnalysisEnabled

        // Faces, weapons and motion are independent, so they run concurrently
        // rather than in sequence. Weapon inference is staggered onto alternate
        // cycles because it is a second full model pass, and three faces plus a
        // weapon pass does not fit one throttle window on a mid-range device.
        val facesTask = async {
            recogniseFaces(
                RecogniseFacesUseCase.Params(
                    frame = frame,
                    profiles = profiles,
                    settings = settings,
                    retainAlignedCrops = wantsAttributes,
                ),
            ).getOrDefault(emptyList())
        }

        val weaponsTask = async {
            if (!settings.weaponDetectionEnabled || analysisCycle % WEAPON_CYCLE_INTERVAL != 0L) {
                null
            } else {
                detectWeapons(
                    DetectWeaponsUseCase.Params(
                        frame = frame,
                        confidenceThreshold = settings.confidenceThreshold,
                    ),
                ).getOrDefault(emptyList())
            }
        }

        val motionTask = async {
            if (!settings.motionDetectionEnabled) {
                MotionResult.NONE
            } else {
                detectMotion(
                    DetectMotionUseCase.Params(
                        frame = frame,
                        intensityThreshold = MOTION_INTENSITY_THRESHOLD,
                    ),
                ).getOrDefault(MotionResult.NONE)
            }
        }

        val faces = facesTask.await()
        val freshWeapons = weaponsTask.await()
        val motion = motionTask.await()

        updateFaceStats(faces)

        recordConfirmedUnknowns(faces, isFrontCamera, now, wantsAttributes)
        freshWeapons?.let { recordWeapons(it, frame, isFrontCamera, now) }
        recordMotion(motion, frame, isFrontCamera, now)

        _uiState.update { state ->
            (state as? LiveUiState.Ready)?.copy(
                detections = faces.map(RecognisedFace::detection),
                // A skipped weapon cycle keeps the previous boxes rather than
                // blanking them, so they do not strobe at half the frame rate.
                weapons = freshWeapons ?: state.weapons,
                motion = motion,
                stats = currentStats(),
                analysisWidth = bitmap.width,
                analysisHeight = bitmap.height,
                isFrontCamera = isFrontCamera,
            ) ?: state
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
            _enrolmentEvent.value =
                EnrolmentEvent.Failed(EnrolmentCapture.Failure.Reason.NO_FACE_DETECTED)
            return
        }

        _uiState.update { state -> (state as? LiveUiState.Ready)?.copy(isEnrolling = true) ?: state }

        viewModelScope.launch {
            val result = enrolFaceFromFrame(
                EnrolFaceFromFrameUseCase.Params(frame = frame, name = name, age = age),
            )

            _enrolmentEvent.value = when (result) {
                is Result.Success -> EnrolmentEvent.Enrolled(result.data.name)
                is Result.Error ->
                    EnrolmentEvent.Failed((result.throwable as? EnrolmentException)?.reason)
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
     * Resets the session counters, the de-duplication keys and the motion
     * baseline: tracking ids restart on the new lens, so carrying any of them
     * across would double-count people and read the lens change itself as one
     * enormous motion event.
     */
    fun flipCamera() {
        seenTrackingIds.clear()
        knownTrackingIds.clear()
        unknownTrackingIds.clear()
        alertedKeys.clear()
        lastAlertAtByKind.clear()
        weaponAlertCount = 0

        viewModelScope.launch { prepareDetectors.resetMotion() }

        _uiState.update { state ->
            (state as? LiveUiState.Ready)?.copy(
                isFrontCamera = !state.isFrontCamera,
                detections = emptyList(),
                weapons = emptyList(),
                motion = MotionResult.NONE,
                stats = SessionStats(),
            ) ?: state
        }
    }

    private suspend fun loadModelsIfNeeded() {
        val current = _uiState.value as? LiveUiState.Ready ?: return
        if (current.faceEngineStatus is EngineStatus.Ready) return

        val result = prepareDetectors(
            PrepareDetectorsUseCase.Params(
                enrolledDimensions = profiles.firstOrNull()?.embeddingSize,
            ),
        )

        val readiness = (result as? Result.Success)?.data ?: return

        _uiState.update { state ->
            (state as? LiveUiState.Ready)?.copy(
                faceEngineStatus = readiness.face,
                weaponEngineStatus = readiness.weapon,
            ) ?: state
        }
    }

    private fun updateFaceStats(faces: List<RecognisedFace>) {
        faces.forEach { face ->
            val detection = face.detection
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
        weapons = weaponAlertCount,
    )

    private suspend fun recordConfirmedUnknowns(
        faces: List<RecognisedFace>,
        isFrontCamera: Boolean,
        now: Long,
        wantsAttributes: Boolean,
    ) {
        faces
            .filter { it.detection.matchStatus == MatchStatus.UNKNOWN }
            .filter { it.detection.trackingId >= 0 }
            .forEach { face ->
                val key = "face:${face.detection.trackingId}"
                if (!claimAlertSlot(key, now)) return@forEach

                // Attributes only on a committed unknown, not on every frame: the
                // classifier cost is per face, and a face still being voted on may
                // never become an alert at all.
                val attributes = if (wantsAttributes && face.isAnalysable) {
                    analyseAttributes(AnalyseAttributesUseCase.Params(face))
                        .getOrDefault(FaceAttributes.NOT_ASSESSED)
                } else {
                    FaceAttributes.NOT_ASSESSED
                }

                val snapshotUri = latestFrame?.let { frame ->
                    captureSnapshot(frame, face.detection.boundingBox)
                }

                recordUnknownSighting(
                    RecordUnknownSightingUseCase.Params(
                        confidence = face.detection.confidence,
                        cameraFacing = facing(isFrontCamera),
                        snapshotUri = snapshotUri,
                        attributes = attributes,
                        timestamp = now,
                    ),
                )
            }
    }

    private suspend fun recordWeapons(
        weapons: List<WeaponDetection>,
        frame: FaceFrame,
        isFrontCamera: Boolean,
        now: Long,
    ) {
        weapons.forEach { weapon ->
            // Keyed by class rather than tracking id, which weapons do not have.
            // Same guard, same window.
            if (!claimAlertSlot("weapon:${weapon.weaponType}", now)) return@forEach

            weaponAlertCount++

            recordWeaponSighting(
                RecordWeaponSightingUseCase.Params(
                    weaponType = weapon.weaponType,
                    confidence = weapon.confidence,
                    cameraFacing = facing(isFrontCamera),
                    snapshotUri = captureSnapshot(frame, weapon.boundingBox),
                    timestamp = now,
                ),
            )
        }
    }

    private suspend fun recordMotion(
        motion: MotionResult,
        frame: FaceFrame,
        isFrontCamera: Boolean,
        now: Long,
    ) {
        // The detector's own debounce already collapses continuous movement into
        // one event, so only a new event reaches the guard at all.
        if (!motion.isNewEvent) return
        if (!claimAlertSlot("motion", now)) return

        recordMotionSighting(
            RecordMotionSightingUseCase.Params(
                intensity = motion.intensity,
                cameraFacing = facing(isFrontCamera),
                // Whole frame: a motion event has no subject to crop to.
                snapshotUri = captureSnapshot(frame, region = null),
                timestamp = now,
            ),
        )
    }

    /**
     * The de-duplication guard every alert type shares.
     *
     * Two conditions: a key alerts at most once, and two alerts of the same kind
     * never fall inside the same window. The second is what stops a detector that
     * keeps reassigning tracking ids from bypassing the first.
     *
     * @param key `kind:id`, where the kind decides which window applies.
     * @param now Frame timestamp.
     * @return `true` when the caller may raise an alert.
     */
    private fun claimAlertSlot(key: String, now: Long): Boolean {
        if (key in alertedKeys) return false

        // Zero rather than a sentinel: `now - Long.MIN_VALUE` overflows and would
        // suppress the very first alert of every kind.
        val kind = key.substringBefore(KEY_SEPARATOR)
        val lastOfKind = lastAlertAtByKind[kind] ?: 0L
        if (now - lastOfKind < Constants.Alerting.DUPLICATE_ALERT_WINDOW_MILLIS) return false

        alertedKeys += key
        lastAlertAtByKind[kind] = now
        return true
    }

    private fun facing(isFrontCamera: Boolean) = if (isFrontCamera) FRONT else BACK

    private companion object {
        /**
         * Minimum gap between analyses.
         *
         * A full pass is detection plus, per face, alignment and an inference.
         * Below roughly this interval the pipeline cannot keep up on a mid-range
         * device and the preview starts to stutter.
         */
        const val ANALYSIS_INTERVAL_MILLIS = 350L

        /**
         * Run weapon inference every *n*th cycle.
         *
         * A second full model pass does not fit alongside several faces in one
         * window. Halving its rate costs at most one throttle period of latency on
         * a weapon that is, by nature, in frame for more than 700 ms.
         */
        const val WEAPON_CYCLE_INTERVAL = 2L

        /** Changed-pixel fraction that counts as motion. */
        const val MOTION_INTENSITY_THRESHOLD = 0.02f

        /** Splits an alert key into its kind and its identifier. */
        const val KEY_SEPARATOR = ':'

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
