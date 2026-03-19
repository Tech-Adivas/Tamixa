package com.tamixa.api.admin.dto

import java.time.Instant

data class AdminInvoiceDto(
    val id: Long,
    val providerInvoiceId: String,
    val parentId: Long,
    val email: String,
    val amount: Double,
    val currency: String,
    val status: String,
    val plan: String,
    val dueDate: String,
    val paidAt: Instant?
)
