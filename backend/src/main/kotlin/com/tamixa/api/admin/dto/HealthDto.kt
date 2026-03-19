package com.tamixa.api.admin.dto

data class HealthDto(
    val status: String,
    val components: Map<String, ComponentHealth>?
)

data class ComponentHealth(
    val status: String,
    val details: Map<String, Any>? = null
)
