package com.tamixa.ui.viewmodel

import com.tamixa.network.ShortContentApi
import com.tamixa.network.ShortContentResponseDto
import com.tamixa.util.TamixaConstants
import com.tamixa.util.TamixaLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ShortContentViewModel(
    private val shortContentApi: ShortContentApi,
    private val scope: CoroutineScope
) {
    private val _items = MutableStateFlow<List<ShortContentResponseDto>>(emptyList())
    val items: StateFlow<List<ShortContentResponseDto>> = _items.asStateFlow()

    private val _dailyItem = MutableStateFlow<ShortContentResponseDto?>(null)
    val dailyItem: StateFlow<ShortContentResponseDto?> = _dailyItem.asStateFlow()

    private val _types = MutableStateFlow<List<String>>(emptyList())
    val types: StateFlow<List<String>> = _types.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadTypes() {
        scope.launch {
            _error.value = null
            try {
                _types.value = shortContentApi.getTypes()
            } catch (e: Exception) {
                TamixaLog.w("ShortContentViewModel", "loadTypes failed", e)
                _types.value = DEFAULT_TYPES
            }
        }
    }

    fun loadList(type: String, language: String = TamixaConstants.DEFAULT_LANGUAGE) {
        scope.launch {
            _loading.value = true
            _error.value = null
            try {
                _items.value = shortContentApi.list(type, language, page = 0, size = 50)
            } catch (e: Exception) {
                TamixaLog.w("ShortContentViewModel", "loadList type=$type failed", e)
                _items.value = emptyList()
                _error.value = e.message ?: "Failed to load"
            } finally {
                _loading.value = false
            }
        }
    }

    fun loadDaily(type: String, language: String = TamixaConstants.DEFAULT_LANGUAGE) {
        scope.launch {
            _error.value = null
            try {
                _dailyItem.value = shortContentApi.getDaily(type, language)
            } catch (e: Exception) {
                TamixaLog.w("ShortContentViewModel", "loadDaily type=$type failed", e)
                _dailyItem.value = null
            }
        }
    }

    companion object {
        private val DEFAULT_TYPES = listOf(
            "RIDDLE", "THOUGHT_FOR_THE_DAY", "JOKE", "PROVERB", "FUN_FACT",
            "TONGUE_TWISTER", "WORD_OF_THE_DAY", "BRAIN_TEASER", "AFFIRMATION", "QUOTE"
        )
    }
}
