package com.tamixa.ui.state

sealed interface UiState<out T> {
    /** No request in progress and no result (e.g. initial state for generate). */
    data object Idle : UiState<Nothing>
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String, val throwable: Throwable? = null) : UiState<Nothing>
}

fun <T> UiState<T>.dataOrNull(): T? = (this as? UiState.Success)?.data
