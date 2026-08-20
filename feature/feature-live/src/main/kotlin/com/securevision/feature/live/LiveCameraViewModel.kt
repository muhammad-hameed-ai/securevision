package com.securevision.feature.live

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securevision.core.common.result.Result
import com.securevision.core.common.result.getOrDefault
import com.securevision.core.common.result.getOrNull
import com.securevision.core.domain.alerting.AlertGate
import com.securevision.core.domain.alerting.AlertRequest
import com.securevision.core.domain.alerting.NotificationOutcome
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
import com.securevision.core.domain.usecase.live.AlertOutcome
import com.securevision.core.domain.usecase.live.PrepareDetectorsUseCase
import com.securevision.core.domain.usecase.live.RaiseAlertUseCase
import com.securevision.core.domain.usecase.live.RecogniseFacesUseCase
import com.securevision.core.domain.usecase.live.SilenceAlarmUseCase
import com.securevision.core.domain.usecase.live.SoundAlarmUseCase
import com.securevision.core.domain.usecase.live.wasRaised
import com.securevision.core.domain.usecase.profile.GetEnrolledProfilesUseCase
import com.securevision.core.domain.usecase.recording.SaveRecordingUseCase
import com.securevision.core.domain.usecase.settings.ObserveSettingsUseCase
import com.securevision.core.model.AppSettings
import com.securevision.core.model.EnrolledProfile
import com.securevision.core.model.FaceAttributes
import com.securevision.core.model.MatchStatus
import com.securevision.core.model.MotionResult
import com.securevision.core.model.Recording
import com.securevision.core.model.Severity
import com.securevision.core.model.WeaponDetection
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
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
    private val saveRecording: SaveRecordingUseCase,
    private val raiseAlert: RaiseAlertUseCase,
    private val silenceAlarm: SilenceAlarmUseCase,
    private val soundAlarm: SoundAlarmUseCase,
    private val alertGate: AlertGate,
    getEnrolledProfiles: GetEnrolledProfilesUseCase,
    getEnrolledProfileCount: GetEnrolledProfileCountUseCase,
    observeSettings: ObserveSettingsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<LiveUiState>(LiveUiState.Loading)

    /** Current screen state. */
    val uiState: StateFlow<LiveUiState> = _uiState.asStateFlow()

    private var profiles: List<EnrolledProfile> = emptyList()
    private var settings: AppSettings = AppSettings()

    /** Most recent frame, retained so an alert snapshot can crop what was on screen. */
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

    /** When the lens last changed, for the short re-entry debounce. */
    private var lastFlipAt = 0L

    /**
     * Whether the user has silenced the alarm for the weapon currently in frame.
     *
     * Without it, re-arming every cycle would immediately override the Silence
     * button and the control would appear broken. Cleared once no weapon is
     * detected, so the next sighting sounds normally.
     */
    private var alarmSilencedByUser = false

    private val seenTrackingIds = mutableSetOf<Int>()
    private val knownTrackingIds = mutableSetOf<Int>()
    private val unknownTrackingIds = mutableSetOf<Int>()
    private var weaponAlertCount = 0

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

        // Belt and braces: a frame proves the new lens is live even if the bind
        // callback never arrived.
        clearSwitchingState()

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
                        // Weapons use their own floor, not the face threshold. A
                        // weapon detection raises a CRITICAL alarm, so a false
                        // positive is far more expensive here than a missed
                        // low-confidence frame on an object that stays in view.
                        confidenceThreshold = WEAPON_CONFIDENCE_THRESHOLD,
                    ),
                ).getOrDefault(emptyList())
            }
        }

        val motionTask = async {
            // Every fourth cycle. Motion already debounces over three seconds, so
            // sampling it four times a second bought nothing and competed with
            // the detectors the operator is actually watching.
            if (!settings.motionDetectionEnabled ||
                analysisCycle % MOTION_CYCLE_INTERVAL != 0L
            ) {
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

        // Published the moment it lands, before the other detectors are awaited.
        //
        // Previously all three were awaited and then one state update was made,
        // so a finished face verdict sat waiting on a weapon inference — which is
        // why a face resolved from amber to red or green quickly with no weapon
        // in frame and slowly with one. The detectors are independent; their
        // results should reach the screen independently too.
        updateFaceStats(faces)

        _uiState.update { state ->
            (state as? LiveUiState.Ready)?.copy(
                detections = faces.map(RecognisedFace::detection),
                stats = currentStats(),
                analysisWidth = bitmap.width,
                analysisHeight = bitmap.height,
                isFrontCamera = isFrontCamera,
            ) ?: state
        }

        faces.forEach { face ->
            if (face.detection.matchStatus != MatchStatus.PROCESSING) {
                Log.i(
                    TIMING_TAG,
                    "face ${face.detection.trackingId} resolved ${face.detection.matchStatus} " +
                        "in ${System.currentTimeMillis() - now}ms",
                )
            }
        }

        val freshWeapons = weaponsTask.await()
        val motion = motionTask.await()

        // Measured, not estimated. The pipeline slowed by a factor of four when
        // the analysis resolution was raised, and nothing in the app said so —
        // it simply felt heavy. One line per cycle makes the next regression
        // visible the moment it lands.
        val elapsed = System.currentTimeMillis() - now
        if (analysisCycle % TIMING_LOG_INTERVAL == 0L) {
            Log.i(
                TIMING_TAG,
                "cycle=$analysisCycle total=${elapsed}ms " +
                    "frame=${bitmap.width}x${bitmap.height} " +
                    "faces=${faces.size} " +
                    "weapon=${if (freshWeapons == null) "skipped" else "${freshWeapons.size}"} " +
                    "motion=${if (analysisCycle % MOTION_CYCLE_INTERVAL == 0L) "ran" else "skipped"}",
            )
        }

        recordConfirmedUnknowns(faces, isFrontCamera, now, wantsAttributes)
        logConfirmedKnowns(faces, isFrontCamera, now)
        freshWeapons?.let { recordWeapons(it, frame, isFrontCamera, now) }
        recordMotion(motion, frame, isFrontCamera, now)

        // Second update, carrying only what the slower detectors produced. The
        // face boxes are already on screen by this point.
        _uiState.update { state ->
            (state as? LiveUiState.Ready)?.copy(
                // A skipped weapon cycle keeps the previous boxes rather than
                // blanking them, so they do not strobe at half the frame rate.
                weapons = freshWeapons ?: state.weapons,
                motion = motion,
                stats = currentStats(),
            ) ?: state
        }
    }

    /**
     * Reports the outcome of a camera bind.
     *
     * This is what ends a lens switch. Waiting for an analysed frame instead —
     * as the previous version did — meant the spinner and the disabled button
     * outlived the switch whenever analysis was slow or stalled, which is what
     * made taps appear to be dropped.
     *
     * @param outcome What the camera actually managed to bind.
     */
    fun onCameraBound(outcome: String) {
        Log.i(FLIP_TAG, "rebind finished: $outcome")
        clearSwitchingState()
    }

    /** Drops the switching state, if one is in progress. */
    private fun clearSwitchingState() {
        _uiState.update { state ->
            (state as? LiveUiState.Ready)
                ?.takeIf { ready -> ready.isSwitchingCamera }
                ?.copy(isSwitchingCamera = false)
                ?: state
        }
    }

    /**
     * Reflects the recorder's state on screen.
     *
     * The recorder itself lives with the camera, because CameraX binds it to the
     * preview's lifecycle; the ViewModel only mirrors what it reports so the
     * screen can render a timer and the state survives recomposition.
     *
     * @param isRecording Whether capture is running.
     * @param elapsedMillis How long it has been running.
     */
    fun onRecordingStateChange(isRecording: Boolean, elapsedMillis: Long) {
        _uiState.update { state ->
            (state as? LiveUiState.Ready)?.copy(
                isRecording = isRecording,
                recordingElapsedMillis = elapsedMillis,
            ) ?: state
        }
    }

    /**
     * Registers a finished clip.
     *
     * Called once the recorder has closed the file. Writing the row earlier would
     * put a zero-length clip in the gallery that cannot be played.
     *
     * @param recording Metadata for the completed clip.
     */
    fun onRecordingFinished(recording: Recording) {
        viewModelScope.launch {
            saveRecording(recording)
        }
    }

    /**
     * Stops a sounding critical alarm.
     *
     * Silences the tone only. The alert stays recorded and the notification stays
     * in the shade — silencing is the user saying "I have heard it", not "that did
     * not happen".
     */
    fun silenceAlarm() {
        // Latched so the next detection cycle does not immediately re-arm what
        // the user just switched off. Cleared when the weapon leaves frame.
        alarmSilencedByUser = true
        silenceAlarm.invoke()
        _uiState.update { state ->
            (state as? LiveUiState.Ready)?.copy(isAlarmSounding = false) ?: state
        }
    }

    /**
     * Stops the alarm when the screen goes away.
     *
     * Without this a critical alarm raised just before the user navigated back
     * would keep sounding with no visible control to stop it, since detection —
     * and therefore the Silence button — only exists on this screen.
     */
    override fun onCleared() {
        silenceAlarm.invoke()
        super.onCleared()
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
        val current = _uiState.value as? LiveUiState.Ready ?: return
        val now = System.currentTimeMillis()

        Log.i(
            FLIP_TAG,
            "tap received: front=${current.isFrontCamera} switching=${current.isSwitchingCamera} " +
                "sinceLastFlip=${now - lastFlipAt}ms",
        )

        // A short re-entry window, not a lock held until the camera reports back.
        // The previous version rejected taps for as long as `isSwitchingCamera`
        // was true, and that flag was only cleared by an *analysed frame* — so if
        // analysis stalled during the rebind the button ate every tap until a
        // three-second timeout expired. That is the "needs four or five taps"
        // report: the taps were arriving and being discarded.
        if (now - lastFlipAt < FLIP_DEBOUNCE_MILLIS) {
            Log.w(FLIP_TAG, "tap ignored: within ${FLIP_DEBOUNCE_MILLIS}ms debounce")
            return
        }
        lastFlipAt = now

        // A pass that never finished would otherwise leave this latched true and
        // stop every subsequent frame being analysed — which also silently stops
        // detection after a flip.
        if (isAnalysing.getAndSet(false)) {
            Log.w(FLIP_TAG, "an analysis pass was still in flight; latch reset")
        }

        seenTrackingIds.clear()
        knownTrackingIds.clear()
        unknownTrackingIds.clear()
        weaponAlertCount = 0

        // The gate is shared, so it is cleared through its own contract rather
        // than by this screen keeping a private copy of the claims.
        alertGate.reset()

        viewModelScope.launch {
            // Vote history is keyed by tracking id, and the new lens restarts
            // those ids from scratch. Without this the first person seen after a
            // flip inherits the previous person's votes — because `retainOnly`
            // keeps id 1's history precisely when the new lens reuses id 1.
            prepareDetectors.resetTracking()
            prepareDetectors.resetMotion()
        }

        // Backstop only. The switching state is normally cleared by the bind
        // result, which is deterministic; this covers a bind that never reports.
        viewModelScope.launch {
            delay(SWITCH_TIMEOUT_MILLIS)
            if ((_uiState.value as? LiveUiState.Ready)?.isSwitchingCamera == true) {
                Log.w(FLIP_TAG, "no bind result after ${SWITCH_TIMEOUT_MILLIS}ms; clearing spinner")
                clearSwitchingState()
            }
        }

        // Flipped immediately rather than after the camera returns. CameraX has
        // to unbind and rebind to change lens, which takes long enough that a
        // button which does nothing until it finishes reads as broken.
        _uiState.update { state ->
            (state as? LiveUiState.Ready)?.copy(
                isFrontCamera = !state.isFrontCamera,
                isSwitchingCamera = true,
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

                raise(
                    AlertRequest.unknownPerson(
                        trackingId = face.detection.trackingId,
                        confidence = face.detection.confidence,
                        cameraFacing = facing(isFrontCamera),
                        snapshotUri = snapshotUri,
                        attributes = attributes,
                        timestamp = now,
                    ),
                )
            }
    }

    /**
     * Records each recognised person as a visible, silent alert.
     *
     * LOW severity, which is doing real work: it appears in the alerts list so a
     * recognition can be seen and audited, while producing no tone (the synth is
     * silent below HIGH) and no notification (`notify = false`). A recognition
     * that leaves no trace is indistinguishable from the app having missed the
     * person entirely.
     */
    private suspend fun logConfirmedKnowns(
        faces: List<RecognisedFace>,
        isFrontCamera: Boolean,
        now: Long,
    ) {
        faces
            .filter { it.detection.matchStatus == MatchStatus.KNOWN }
            .forEach { face ->
                val profileId = face.detection.profileId ?: return@forEach

                val snapshotUri = latestFrame?.let { frame ->
                    captureSnapshot(frame, face.detection.boundingBox)
                }

                raise(
                    AlertRequest.knownPerson(
                        profileId = profileId,
                        profileName = face.detection.profileName.orEmpty(),
                        confidence = face.detection.confidence,
                        cameraFacing = facing(isFrontCamera),
                        snapshotUri = snapshotUri,
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
        // Every confidence, every cycle. A false positive is only diagnosable if
        // its actual score is visible — "shoes triggered it" and "shoes scored
        // 0.71" call for completely different responses.
        weapons.forEach { weapon ->
            Log.i(
                TIMING_TAG,
                "weapon ${weapon.weaponType} confidence=${"%.3f".format(weapon.confidence)} " +
                    "box=${"%.2f".format(weapon.boundingBox.left)}," +
                    "${"%.2f".format(weapon.boundingBox.top)}",
            )
        }

        // Re-arm the alarm on every cycle a weapon is still present, separately
        // from the alert record. AlertGate claims `weapon:<type>` once and never
        // again — correct for the record, since nobody wants forty rows for one
        // weapon, but it meant the alarm sounded once and fell silent while the
        // weapon was still in frame.
        if (weapons.isNotEmpty()) {
            rearmWeaponAlarm()
        } else {
            // The weapon has gone. Silencing applied to that sighting, so a new
            // one must be able to sound again.
            alarmSilencedByUser = false
        }

        weapons.forEach { weapon ->
            val outcome = raise(
                AlertRequest.weapon(
                    weaponType = weapon.weaponType,
                    confidence = weapon.confidence,
                    cameraFacing = facing(isFrontCamera),
                    snapshotUri = captureSnapshot(frame, weapon.boundingBox),
                    timestamp = now,
                ),
            )

            // Counted only when the gate actually let it through, so the stat
            // matches the number of alerts the user was shown.
            if (outcome?.wasRaised == true) weaponAlertCount++
        }
    }

    private suspend fun recordMotion(
        motion: MotionResult,
        frame: FaceFrame,
        isFrontCamera: Boolean,
        now: Long,
    ) {
        // The detector's own debounce already collapses continuous movement into
        // one event, so only a new event reaches the gate at all.
        if (!motion.isNewEvent) return

        raise(
            AlertRequest.motion(
                intensity = motion.intensity,
                cameraFacing = facing(isFrontCamera),
                // Whole frame: a motion event has no subject to crop to.
                snapshotUri = captureSnapshot(frame, region = null),
                timestamp = now,
            ),
        )
    }

    /**
     * Keeps the critical alarm sounding while a weapon remains in frame.
     *
     * Independent of [AlertGate], which de-duplicates the *record*. The alarm is
     * a live indication of a present danger rather than a log entry, so it
     * follows the weapon rather than the alert. The player itself ignores a
     * re-arm while the same tone is already looping, so this is cheap and cannot
     * stack tracks.
     *
     * Stopped only by the user's Silence button, or by the weapon leaving frame.
     */
    private fun rearmWeaponAlarm() {
        if (alarmSilencedByUser) return

        viewModelScope.launch {
            soundAlarm(Severity.CRITICAL)

            _uiState.update { state ->
                (state as? LiveUiState.Ready)?.copy(isAlarmSounding = true) ?: state
            }
        }
    }

    /**
     * Hands one request to the single alerting path.
     *
     * De-duplication, persistence, the alarm and the notification all happen
     * inside [RaiseAlertUseCase]; this ViewModel no longer owns a guard of its
     * own. What comes back is used only for the parts that are genuinely the
     * screen's business: whether to count the alert, whether to offer Silence, and
     * whether to explain that notifications are switched off at the system level.
     */
    private suspend fun raise(request: AlertRequest): AlertOutcome? {
        val outcome = raiseAlert(request).getOrNull() ?: return null

        if (outcome is AlertOutcome.Raised) {
            _uiState.update { state ->
                (state as? LiveUiState.Ready)?.copy(
                    isAlarmSounding = outcome.alarmSounding,
                    notificationsBlocked =
                        outcome.notification == NotificationOutcome.PERMISSION_DENIED,
                ) ?: state
            }
        }

        return outcome
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
        const val ANALYSIS_INTERVAL_MILLIS = 200L

        /**
         * Run weapon inference every *n*th cycle.
         *
         * A second full model pass does not fit alongside several faces in one
         * window. Halving its rate costs at most one throttle period of latency on
         * a weapon that is, by nature, in frame for more than 700 ms.
         */
        const val WEAPON_CYCLE_INTERVAL = 2L

        /**
         * Run motion detection every *n*th cycle.
         *
         * Motion debounces its own events over three seconds, so evaluating it on
         * every cycle produced no extra sensitivity — it only competed for the
         * frame budget with detection, which is what the operator watches.
         */
        const val MOTION_CYCLE_INTERVAL = 4L

        /** Changed-pixel fraction that counts as motion. */
        const val MOTION_INTENSITY_THRESHOLD = 0.02f

        /**
         * Minimum score for a weapon detection.
         *
         * Higher than the face threshold and deliberately not user-tunable. This
         * is the one detector that sounds a repeating alarm, and the shipped
         * model reports mAP50 0.80 — 0.70 keeps the confident detections and
         * discards the tail where a phone or a remote control starts to look
         * like a handgun.
         */
        const val WEAPON_CONFIDENCE_THRESHOLD = 0.70f

        /**
         * How long a lens change may block the flip control.
         *
         * Generous: a cold camera on a slow device can take a second. Its job is
         * only to stop a failed bind disabling the button permanently.
         */
        const val SWITCH_TIMEOUT_MILLIS = 3_000L

        /**
         * Minimum gap between accepted flips.
         *
         * Long enough to swallow a double-tap, short enough that a deliberate
         * second press is never lost. Deliberately not tied to how long the
         * camera takes: a tap must never be discarded because hardware is slow.
         */
        const val FLIP_DEBOUNCE_MILLIS = 400L

        /** Logcat tag for diagnosing the flip path end to end. */
        const val FLIP_TAG = "CameraFlip"

        /** Logcat tag for per-cycle pipeline cost. */
        const val TIMING_TAG = "PipelineTiming"

        /** Log every *n*th cycle: enough to see the trend, not enough to flood. */
        const val TIMING_LOG_INTERVAL = 5L

        const val FRONT = "front"
        const val BACK = "back"
    }
}
