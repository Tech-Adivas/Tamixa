package com.araro.ui.viewmodel

import com.araro.domain.Child
import com.araro.domain.CreateChildRequest
import com.araro.util.AraroLog
import com.araro.repository.ChildRepository
import com.araro.ui.state.UiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChildViewModel(
    private val childRepository: ChildRepository,
    private val scope: CoroutineScope
) {
    private val _children = MutableStateFlow<UiState<List<Child>>>(UiState.Loading)
    val children: StateFlow<UiState<List<Child>>> = _children.asStateFlow()

    private val _createState = MutableStateFlow<UiState<Child>>(UiState.Loading)
    val createState: StateFlow<UiState<Child>> = _createState.asStateFlow()

    fun loadChildren() {
        scope.launch {
            _children.value = UiState.Loading
            childRepository.list()
                .fold(
                    onSuccess = { _children.value = UiState.Success(it) },
                    onFailure = {
                        AraroLog.w("ChildViewModel", "loadChildren failed", it)
                        _children.value = UiState.Error(it.message ?: "Failed to load children", it)
                    }
                )
        }
    }

    fun createChild(request: CreateChildRequest) {
        scope.launch {
            _createState.value = UiState.Loading
            childRepository.create(request)
                .fold(
                    onSuccess = { _createState.value = UiState.Success(it) },
                    onFailure = {
                        AraroLog.w("ChildViewModel", "createChild failed", it)
                        _createState.value = UiState.Error(it.message ?: "Failed to create child", it)
                    }
                )
        }
    }
}
