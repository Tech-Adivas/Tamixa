package com.tamixa.library

import com.tamixa.util.TamixaConstants

/**
 * Library translation / narration pipeline language codes (aligned with backend `app.translation-pipeline`
 * target set and admin `LIBRARY_TAB_LANGUAGES`). Used to normalize API `language` query params so unknown
 * codes do not hit the server as arbitrary strings.
 */
object LibraryPipelineLanguages {
    val CODES: Set<String> = setOf("ta", "en", "hi", "te", "kn", "ml")

    /**
     * Returns [code] lowercased if it is a known pipeline language; otherwise [TamixaConstants.DEFAULT_LANGUAGE].
     */
    fun normalizeForApi(code: String): String {
        val c = code.trim().lowercase().take(10)
        return if (c in CODES) c else TamixaConstants.DEFAULT_LANGUAGE
    }
}
