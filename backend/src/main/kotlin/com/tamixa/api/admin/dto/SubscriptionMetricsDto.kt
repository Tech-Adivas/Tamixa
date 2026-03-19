package com.tamixa.api.admin.dto

data class SubscriptionMetricsDto(
    val activeSubscriptions: Int,
    val trialCount: Int,
    val planDistribution: Map<String, Int>,
    val mrr: java.math.BigDecimal,
    val trialConversionRate: java.math.BigDecimal? = null,
    val churnRate: java.math.BigDecimal? = null
)
