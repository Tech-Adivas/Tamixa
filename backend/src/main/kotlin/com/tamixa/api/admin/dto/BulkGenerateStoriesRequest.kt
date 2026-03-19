package com.tamixa.api.admin.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size

data class BulkGenerateStoriesRequest(
    @field:Size(max = 20)
    val languages: List<String>? = null,
    @field:Size(max = 30)
    val categories: List<String>? = null,
    @field:Min(1) @field:Max(25)
    val totalStories: Int? = null,
    val publish: Boolean? = null
)
