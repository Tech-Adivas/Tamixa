package com.araro.api.admin.dto

import jakarta.validation.constraints.Size

/** Request for republish: clear audio and reprocess selected languages. Empty = all. */
data class RepublishRequest(
    @field:Size(max = 20)
    val languages: List<String>? = null
)
