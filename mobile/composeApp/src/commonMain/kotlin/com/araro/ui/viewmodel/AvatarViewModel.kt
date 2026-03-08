package com.araro.ui.viewmodel

import com.araro.repository.AvatarRepository
import com.araro.ui.state.UiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AvatarViewModel(
    private val avatarRepository: AvatarRepository,
    private val scope: CoroutineScope
) {
    private val _uploadState = MutableStateFlow<UiState<String>>(UiState.Loading)
    val uploadState: StateFlow<UiState<String>> = _uploadState.asStateFlow()

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
                    onFailure = { _uploadState.value = UiState.Error(it.message ?: "Upload failed", it) }
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
                    onFailure = { _uploadState.value = UiState.Error(it.message ?: "Delete failed", it) }
                )
        }
    }
}
