package com.tamixa.ui.viewmodel

import com.tamixa.domain.VoiceDeleteResult
import com.tamixa.domain.VoiceLimitReachedException
import com.tamixa.domain.VoiceProfile
import com.tamixa.platform.writeTempAudioFile
import com.tamixa.repository.VoiceRepository
import com.tamixa.ui.AppMessageNotifier
import com.tamixa.ui.errorMessageForUser
import com.tamixa.ui.state.UiState
import com.tamixa.ui.strings.Strings
import com.tamixa.util.TamixaLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Playback of a saved voice recording: [fileUri] is set once the audio is downloaded to a local file. */
data class SavedVoicePlayback(
    val profileId: Long,
    val isLoading: Boolean = false,
    val fileUri: String? = null,
    val errorMessage: String? = null
)

class VoiceViewModel(
    private val voiceRepository: VoiceRepository,
    private val appMessageNotifier: AppMessageNotifier,
    private val scope: CoroutineScope
) {
    private val _uploadState = MutableStateFlow<UiState<VoiceProfile>>(UiState.Success(com.tamixa.ui.viewmodel.VoiceViewModel.IDLE_PROFILE))
    val uploadState: StateFlow<UiState<VoiceProfile>> = _uploadState.asStateFlow()

    private var returnToStoryId: Long? = null
    private var returnToStorySource: String? = null

    fun setReturnToStory(storyId: Long, storySource: String) {
        returnToStoryId = storyId
        returnToStorySource = storySource
    }

    fun hasReturnToStory(): Boolean = returnToStoryId != null && returnToStorySource != null

    fun getAndClearReturnToStory(): Pair<Long, String>? {
        val id = returnToStoryId
        val src = returnToStorySource
        returnToStoryId = null
        returnToStorySource = null
        return if (id != null && src != null) Pair(id, src) else null
    }

    private val _refreshVoicesTrigger = MutableStateFlow(0)
    val refreshVoicesTrigger: StateFlow<Int> = _refreshVoicesTrigger.asStateFlow()

    fun notifyVoiceUploadCompleted() {
        _refreshVoicesTrigger.value += 1
    }

    companion object {
        /** Sentinel so we don't show upload progress or success on first open */
        val IDLE_PROFILE = VoiceProfile(id = -1L, parentId = -1L, createdAt = "")
    }

    private val _profiles = MutableStateFlow<UiState<List<VoiceProfile>>>(UiState.Success(emptyList()))
    val profiles: StateFlow<UiState<List<VoiceProfile>>> = _profiles.asStateFlow()

    /** Uploads one recording. Ignored while another upload is running, so a double tap can't send it twice. */
    fun uploadVoice(fileBytes: ByteArray, fileName: String) {
        if (_uploadState.value is UiState.Loading) {
            TamixaLog.i("VoiceViewModel", "Upload ignored: another upload is in progress")
            return
        }
        _uploadState.value = UiState.Loading
        scope.launch {
            voiceRepository.upload(fileBytes, fileName)
                .fold(
                    onSuccess = { _uploadState.value = UiState.Success(it) },
                    onFailure = {
                        if (it is VoiceLimitReachedException) {
                            _uploadState.value = UiState.Error(Strings.voiceLimitReached(), it)
                        } else {
                            _uploadState.value = UiState.Error(errorMessageForUser(it), it)
                            appMessageNotifier.showError()
                        }
                    }
                )
        }
    }

    private val _hasVoiceConsent = MutableStateFlow(false)
    /** Whether the parent agreed to voice cloning; the app asks before the first recording or upload. */
    val hasVoiceConsent: StateFlow<Boolean> = _hasVoiceConsent.asStateFlow()

    fun loadVoiceConsent() {
        scope.launch {
            voiceRepository.hasVoiceCloningConsent()
                .onSuccess { _hasVoiceConsent.value = it }
                .onFailure { TamixaLog.w("VoiceViewModel", "Loading voice consent failed", it) }
        }
    }

    /** Records the parent's voice cloning consent on the server. Returns false (and shows an error) when that fails. */
    suspend fun giveVoiceConsent(): Boolean =
        voiceRepository.recordVoiceCloningConsent().fold(
            onSuccess = {
                TamixaLog.i("VoiceViewModel", "Voice cloning consent recorded")
                _hasVoiceConsent.value = true
                true
            },
            onFailure = {
                if (it is kotlinx.coroutines.CancellationException) throw it
                TamixaLog.w("VoiceViewModel", "Recording voice consent failed", it)
                appMessageNotifier.showError()
                false
            }
        )

    private val _deletingProfileIds = MutableStateFlow<Set<Long>>(emptySet())
    /** Voices whose delete is in progress (their delete button is disabled). */
    val deletingProfileIds: StateFlow<Set<Long>> = _deletingProfileIds.asStateFlow()

    /**
     * Deletes one of the parent's voices on the server (stored recording + profile), then reloads the list.
     * The server refuses (IN_USE) when a generated story still uses the voice; nothing is deleted then.
     * [delete] performs the delete; the app passes LocalMediaSyncRepository.deleteServerVoice, which also removes
     * the device recording the voice was uploaded from.
     */
    fun deleteProfile(
        profileId: Long,
        delete: suspend (Long) -> Result<VoiceDeleteResult> = { voiceRepository.delete(it) }
    ) {
        if (profileId in _deletingProfileIds.value) return
        _deletingProfileIds.value = _deletingProfileIds.value + profileId
        if (_savedPlayback.value?.profileId == profileId) stopSavedPlayback()
        scope.launch {
            try {
                delete(profileId)
                    .fold(
                        onSuccess = { result ->
                            TamixaLog.i("VoiceViewModel", "Voice delete profileId=$profileId: $result")
                            when (result) {
                                VoiceDeleteResult.DELETED, VoiceDeleteResult.NOT_FOUND -> appMessageNotifier.show(Strings.voiceDeleted())
                                VoiceDeleteResult.IN_USE -> appMessageNotifier.show(Strings.voiceInUseCannotDelete())
                            }
                        },
                        onFailure = {
                            if (it is kotlinx.coroutines.CancellationException) throw it
                            TamixaLog.w("VoiceViewModel", "Voice delete profileId=$profileId failed", it)
                            appMessageNotifier.showError()
                        }
                    )
            } finally {
                _deletingProfileIds.value = _deletingProfileIds.value - profileId
            }
            loadProfiles()
        }
    }

    private val _savedPlayback = MutableStateFlow<SavedVoicePlayback?>(null)
    val savedPlayback: StateFlow<SavedVoicePlayback?> = _savedPlayback.asStateFlow()
    private var savedPlaybackJob: Job? = null

    /** Downloads the saved reference recording for [profileId] and exposes it as a local file for playback. */
    fun playSavedProfile(profileId: Long) {
        savedPlaybackJob?.cancel()
        _savedPlayback.value = SavedVoicePlayback(profileId = profileId, isLoading = true)
        savedPlaybackJob = scope.launch {
            voiceRepository.getReferenceAudio(profileId)
                .fold(
                    onSuccess = { bytes ->
                        val uri = writeTempAudioFile(bytes, "saved_voice_$profileId.m4a")
                        _savedPlayback.value = if (uri != null) {
                            SavedVoicePlayback(profileId = profileId, fileUri = uri)
                        } else {
                            SavedVoicePlayback(profileId = profileId, errorMessage = com.tamixa.ui.strings.Strings.somethingWentWrong())
                        }
                    },
                    onFailure = {
                        // runCatching also captures cancellation (user pressed stop); don't surface it as an error.
                        if (it is kotlinx.coroutines.CancellationException) return@fold
                        _savedPlayback.value = SavedVoicePlayback(profileId = profileId, errorMessage = errorMessageForUser(it))
                        appMessageNotifier.showError()
                    }
                )
        }
    }

    fun stopSavedPlayback() {
        savedPlaybackJob?.cancel()
        savedPlaybackJob = null
        _savedPlayback.value = null
    }

    fun loadProfiles() {
        scope.launch {
            _profiles.value = UiState.Loading
            voiceRepository.list()
                .fold(
                    onSuccess = { _profiles.value = UiState.Success(it) },
                    onFailure = {
                        val msg = errorMessageForUser(it)
                        _profiles.value = UiState.Error(msg, it)
                        appMessageNotifier.showError()
                    }
                )
        }
    }
}
