package com.tamixa.api.admin.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class InteractiveSegmentAudioGenerateRequest(
    @field:Size(max = 10)
    val language: String = "ta",
    @field:Size(max = 120)
    val voiceProfile: String? = "default",
    @field:NotBlank
    @field:Size(max = 100_000)
    val interactiveGraph: String,
    @field:Valid
    @field:Size(min = 1, max = 60)
    val segments: List<InteractiveSegmentScriptInput> = emptyList(),
)

data class InteractiveSegmentScriptInput(
    @field:NotBlank
    @field:Size(max = 80)
    val segmentId: String,
    /** May be blank for a segment in a batch; [InteractiveEpisodeAdminService] marks those as failed per segment. */
    @field:Size(max = 4000)
    val text: String,
    val overwriteExisting: Boolean = false,
)
