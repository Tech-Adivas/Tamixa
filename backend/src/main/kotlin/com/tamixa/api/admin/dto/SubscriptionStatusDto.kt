package com.tamixa.api.admin.dto

data class SubscriptionStatusDto(
    val parentId: Long,
    val email: String,
    val status: String,
    val plan: String?
)
