package com.tamixa.api.admin.dto

data class RevenueRowDto(
    val parentId: Long,
    val email: String,
    val plan: String,
    val subscriptionState: String,
    val monthlyPayment: Double,
    val createdAt: String
)
