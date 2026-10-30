package com.tamixa.api.admin.dto

import jakarta.validation.constraints.Size

/** Question pictures for an interactive story; [segmentIds] empty or null means every question. */
data class InteractiveQuestionImagesRequest(
    @field:Size(max = 50)
    val segmentIds: List<@Size(max = 100) String>? = null,
    @field:Size(max = 2000)
    val customPrompt: String? = null,
)
