package com.tamixa.library

import com.tamixa.util.TamixaConstants
import kotlin.test.Test
import kotlin.test.assertEquals

class LibraryPipelineLanguagesTest {

    @Test
    fun normalizeForApi_knownCodeLowercased() {
        assertEquals("ta", LibraryPipelineLanguages.normalizeForApi("TA"))
        assertEquals("en", LibraryPipelineLanguages.normalizeForApi("en"))
    }

    @Test
    fun normalizeForApi_unknownFallsBackToDefault() {
        assertEquals(TamixaConstants.DEFAULT_LANGUAGE, LibraryPipelineLanguages.normalizeForApi("fr"))
        assertEquals(TamixaConstants.DEFAULT_LANGUAGE, LibraryPipelineLanguages.normalizeForApi(""))
    }
}
