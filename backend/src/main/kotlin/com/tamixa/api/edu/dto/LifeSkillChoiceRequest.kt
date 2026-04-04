package com.tamixa.api.edu.dto

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class LifeSkillChoiceRequest(
    @field:NotNull
    @field:Min(1)
    val libraryStoryId: Long,
    @field:NotNull
    @field:Min(1)
    val childId: Long,
    @field:NotBlank
    @field:Size(max = 128)
    val segmentId: String,
    @field:NotBlank
    @field:Size(max = 128)
    val choiceId: String,
    /** Author-defined skill keys and integer deltas from the interactive graph (capped server-side). */
    val skillDeltas: Map<String, Int>? = null,
)
