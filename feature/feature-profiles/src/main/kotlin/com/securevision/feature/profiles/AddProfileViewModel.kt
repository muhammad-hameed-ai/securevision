package com.securevision.feature.profiles

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securevision.core.common.result.Result
import com.securevision.core.common.result.getOrNull
import com.securevision.core.domain.engine.EngineStatus
import com.securevision.core.domain.engine.EnrolmentCapture
import com.securevision.core.domain.engine.FaceFrame
import com.securevision.core.domain.usecase.live.PrepareDetectorsUseCase
import kotlinx.coroutines.flow.first
import com.securevision.core.domain.usecase.profile.CaptureEnrolmentUseCase
import com.securevision.core.domain.usecase.profile.ProfileRules
import com.securevision.core.domain.usecase.profile.SaveEnrolmentUseCase
import com.securevision.core.domain.usecase.profile.UpdateProfileUseCase
import com.securevision.core.domain.repository.EnrolledProfileRepository
import com.securevision.core.model.AccessLevel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Drives enrolment and editing.
 *
 * One ViewModel for both because they are the same form: editing is enrolment
 * with the fields pre-filled and the face optional. Splitting them would
 * duplicate the validation and the access-level handling for no gain.
 *
 * The captured [EnrolmentCapture.Success] is held here between the shutter press
 * and the save, so the embedding is produced **once**. Re-running it on save
 * would be a second inference over the same pixels for no benefit.
 */
@HiltViewModel
class AddProfileViewModel @Inject constructor(
    private val prepareDetectors: PrepareDetectorsUseCase,
    private val captureEnrolment: CaptureEnrolmentUseCase,
    private val saveEnrolment: SaveEnrolmentUseCase,
    private val updateProfile: UpdateProfileUseCase,
    private val repository: EnrolledProfileRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddProfileUiState())

    /** Current form state. */
    val uiState: StateFlow<AddProfileUiState> = _uiState.asStateFlow()

    private val _saved = MutableStateFlow(false)

    /** Flips true once the profile is stored, so the screen can navigate back. */
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    private var capture: EnrolmentCapture.Success? = null
    private var editingProfileId: String? = null

    init {
        // Enrolment loads the face model itself rather than assuming some other
        // screen already did. It did assume exactly that until now: `prepare()`
        // was only ever called by the live camera, so opening People → Add Person
        // without visiting Live gave "the face model is not installed" for a model
        // that was present and perfectly loadable — nothing had asked for it.
        viewModelScope.launch {
            _uiState.update { state -> state.copy(isPreparing = true) }

            val readiness = prepareDetectors(
                PrepareDetectorsUseCase.Params(
                    enrolledDimensions = repository.getAll().first().firstOrNull()?.embeddingSize,
                ),
            ).getOrNull()

            _uiState.update { state ->
                state.copy(
                    isPreparing = false,
                    // Only now is "unavailable" honest: the model was asked for
                    // and genuinely could not be loaded.
                    modelUnavailable = readiness?.face !is EngineStatus.Ready,
                )
            }
        }
    }

    /**
     * Loads an existing profile for editing.
     *
     * @param profileId Which profile, or `null` to enrol someone new.
     */
    fun load(profileId: String?) {
        if (profileId == null || editingProfileId == profileId) return

        editingProfileId = profileId

        viewModelScope.launch {
            val existing = repository.getById(profileId) ?: return@launch

            _uiState.update { state ->
                state.copy(
                    name = existing.name,
                    age = existing.age.toString(),
                    accessLevel = existing.accessLevel,
                    isWatchlisted = existing.isWatchlisted,
                    isEditing = true,
                )
            }
        }
    }

    /** @param value New name text. */
    fun onNameChange(value: String) {
        _uiState.update { state -> state.copy(name = value, nameError = null) }
    }

    /** @param value New age text; non-digits are ignored so the field cannot hold junk. */
    fun onAgeChange(value: String) {
        if (value.any { character -> !character.isDigit() }) return

        _uiState.update { state -> state.copy(age = value, ageError = null) }
    }

    /** @param level New classification. */
    fun onAccessLevelChange(level: AccessLevel) {
        _uiState.update { state -> state.copy(accessLevel = level) }
    }

    /** Toggles whether sightings of this person are escalated. */
    fun onWatchlistToggle() {
        _uiState.update { state -> state.copy(isWatchlisted = !state.isWatchlisted) }
    }

    /**
     * Runs the captured photo through detect → align → gate → embed.
     *
     * The verdict is shown immediately, with the aligned crop, so a bad shot is
     * caught here rather than weeks later in the match scores.
     *
     * @param bitmap The upright photo from the shutter.
     * @param isFrontCamera Which lens took it.
     */
    fun onPhotoCaptured(bitmap: Bitmap, isFrontCamera: Boolean) {
        _uiState.update { state ->
            state.copy(isCapturing = true, captureFailure = null)
        }

        viewModelScope.launch {
            val frame = FaceFrame(
                bitmap = bitmap,
                isFrontCamera = isFrontCamera,
                timestampMillis = System.currentTimeMillis(),
            )

            val result = captureEnrolment(CaptureEnrolmentUseCase.Params(frame))

            when (val outcome = (result as? Result.Success)?.data) {
                is EnrolmentCapture.Success -> {
                    capture = outcome
                    _uiState.update { state ->
                        state.copy(
                            alignedCrop = outcome.alignedFace,
                            captureFailure = null,
                            isCapturing = false,
                        )
                    }
                }

                is EnrolmentCapture.Failure -> {
                    capture = null
                    _uiState.update { state ->
                        state.copy(
                            alignedCrop = null,
                            captureFailure = outcome.reason,
                            isCapturing = false,
                        )
                    }
                }

                null -> {
                    capture = null
                    _uiState.update { state ->
                        state.copy(
                            alignedCrop = null,
                            captureFailure = EnrolmentCapture.Failure.Reason.MODEL_UNAVAILABLE,
                            isCapturing = false,
                        )
                    }
                }
            }
        }
    }

    /** Discards the current capture so another can be taken. */
    fun onRetakePhoto() {
        capture = null
        _uiState.update { state -> state.copy(alignedCrop = null, captureFailure = null) }
    }

    /**
     * Validates and stores the profile.
     *
     * Editing without a fresh capture updates the details only, leaving the
     * embedding untouched — correcting a spelling must not require the person to
     * come back and stand in front of the camera.
     */
    fun onSave() {
        val state = _uiState.value
        val age = state.age.toIntOrNull()

        val ageError = when {
            age == null -> FieldError.REQUIRED
            age !in ProfileRules.AGE_RANGE -> FieldError.AGE_OUT_OF_RANGE
            else -> null
        }
        val nameError = FieldError.REQUIRED.takeIf { state.name.isBlank() }

        if (nameError != null || ageError != null || age == null) {
            _uiState.update { current ->
                current.copy(nameError = nameError, ageError = ageError)
            }
            return
        }

        _uiState.update { current -> current.copy(isSaving = true) }

        viewModelScope.launch {
            val pending = capture
            val editingId = editingProfileId

            val result = when {
                pending != null -> saveEnrolment(
                    SaveEnrolmentUseCase.Params(
                        capture = pending,
                        name = state.name,
                        age = age,
                        accessLevel = state.accessLevel,
                        isWatchlisted = state.isWatchlisted,
                        replacingProfileId = editingId,
                    ),
                )

                editingId != null -> updateProfile(
                    UpdateProfileUseCase.Params(
                        id = editingId,
                        name = state.name,
                        age = age,
                        accessLevel = state.accessLevel,
                        isWatchlisted = state.isWatchlisted,
                    ),
                )

                // Unreachable through the UI, which requires a face before
                // enabling save. Guarded anyway rather than writing a profile
                // that could never be matched.
                else -> Result.Error(IllegalStateException("no capture"), null)
            }

            _uiState.update { current -> current.copy(isSaving = false) }
            _saved.value = result is Result.Success
        }
    }
}
