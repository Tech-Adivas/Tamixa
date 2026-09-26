package com.tamixa.ui.viewmodel

import com.tamixa.domain.VoiceProfile
import com.tamixa.repository.VoiceRepository
import com.tamixa.ui.AppMessageNotifier
import com.tamixa.ui.errorMessageForUser
import com.tamixa.ui.state.UiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

    fun uploadVoice(fileBytes: ByteArray, fileName: String) {
        scope.launch {
            _uploadState.value = UiState.Loading
            voiceRepository.upload(fileBytes, fileName)
                .fold(
                    onSuccess = { _uploadState.value = UiState.Success(it) },
                    onFailure = {
                        val msg = errorMessageForUser(it)
                        _uploadState.value = UiState.Error(msg, it)
                        appMessageNotifier.showError()
                    }
                )
        }
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
