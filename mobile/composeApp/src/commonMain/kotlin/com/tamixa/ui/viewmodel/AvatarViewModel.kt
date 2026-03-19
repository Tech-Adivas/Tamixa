package com.tamixa.ui.viewmodel

import com.tamixa.repository.AvatarRepository
import com.tamixa.ui.AppMessageNotifier
import com.tamixa.ui.errorMessageForUser
import com.tamixa.ui.state.UiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AvatarViewModel(
    private val avatarRepository: AvatarRepository,
    private val appMessageNotifier: AppMessageNotifier,
    private val scope: CoroutineScope
) {
    private val _uploadState = MutableStateFlow<UiState<String>>(UiState.Success(""))
    val uploadState: StateFlow<UiState<String>> = _uploadState.asStateFlow()

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

    private val _avatarUrl = MutableStateFlow<String?>(null)
    val avatarUrl: StateFlow<String?> = _avatarUrl.asStateFlow()

    fun uploadAvatar(imageBytes: ByteArray, contentType: String = "image/jpeg") {
        scope.launch {
            _uploadState.value = UiState.Loading
            avatarRepository.uploadAvatar(imageBytes, contentType)
                .fold(
                    onSuccess = { url ->
                        _uploadState.value = UiState.Success(url)
                        _avatarUrl.value = url
                    },
                    onFailure = {
                        val msg = errorMessageForUser(it)
                        _uploadState.value = UiState.Error(msg, it)
                        appMessageNotifier.showError()
                    }
                )
        }
    }

    fun loadAvatar() {
        scope.launch {
            avatarRepository.getAvatarUrl()
                .fold(
                    onSuccess = { _avatarUrl.value = it },
                    onFailure = { _avatarUrl.value = null }
                )
        }
    }

    fun deleteAvatar() {
        scope.launch {
            _uploadState.value = UiState.Loading
            avatarRepository.deleteAvatar()
                .fold(
                    onSuccess = {
                        _avatarUrl.value = null
                        _uploadState.value = UiState.Success("")
                    },
                    onFailure = {
                        val msg = errorMessageForUser(it)
                        _uploadState.value = UiState.Error(msg, it)
                        appMessageNotifier.showError()
                    }
                )
        }
    }
}
