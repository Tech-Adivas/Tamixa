package com.araro.ui.viewmodel

import com.araro.domain.VoiceProfile
import com.araro.repository.VoiceRepository
import com.araro.ui.state.UiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VoiceViewModel(
    private val voiceRepository: VoiceRepository,
    private val scope: CoroutineScope
) {
    private val _uploadState = MutableStateFlow<UiState<VoiceProfile>>(UiState.Loading)
    val uploadState: StateFlow<UiState<VoiceProfile>> = _uploadState.asStateFlow()

    private val _profiles = MutableStateFlow<UiState<List<VoiceProfile>>>(UiState.Loading)
    val profiles: StateFlow<UiState<List<VoiceProfile>>> = _profiles.asStateFlow()

    fun uploadVoice(fileBytes: ByteArray, fileName: String) {
        scope.launch {
            _uploadState.value = UiState.Loading
            voiceRepository.upload(fileBytes, fileName)
                .fold(
                    onSuccess = { _uploadState.value = UiState.Success(it) },
                    onFailure = { _uploadState.value = UiState.Error(it.message ?: "Upload failed", it) }
                )
        }
    }

    fun loadProfiles() {
        scope.launch {
            _profiles.value = UiState.Loading
            voiceRepository.list()
                .fold(
                    onSuccess = { _profiles.value = UiState.Success(it) },
                    onFailure = { _profiles.value = UiState.Error(it.message ?: "Failed to load profiles", it) }
                )
        }
    }
}
