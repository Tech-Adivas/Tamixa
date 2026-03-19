package com.tamixa.application.narration

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Estimates cost per story generation for budgeting and alerts.
 * Logs estimated cost; does NOT block or charge—informational only.
 *
 * Scale rationale: At 100k stories, cost visibility prevents runaway spend.
 * Rates are configurable; update when provider pricing changes.
 */
@Service
class CostEstimatorService(
    private val registry: MeterRegistry
) {

    private val log = LoggerFactory.getLogger(javaClass)

    private val costEstimateTotalUsd: Counter = registry.counter("narration_cost_estimate_usd_total")

    /** Approx $/1k tokens (gpt-4o-mini). Update when pricing changes. */
    private val costPer1kInputTokens = 0.00015
    private val costPer1kOutputTokens = 0.0006

    /** Approx $/1k TTS characters (provider-dependent). Update when pricing changes. */
    private val costPer1kTtsChars = 0.016

    data class CostEstimate(
        val aiCostUsd: Double,
        val ttsCostUsd: Double,
        val totalUsd: Double,
        val inputTokens: Int,
        val outputTokens: Int,
        val ttsCharacters: Int
    )

    /**
     * Estimate and log cost for a single story narration.
     * Call after format + TTS complete (idempotent—no side effects besides logging).
     * Metrics: narration_cost_estimate_usd_total counter for cost tracking.
     */
    fun estimateAndLog(
        inputTokens: Int,
        outputTokens: Int,
        ttsCharacters: Int,
        context: String = ""
    ): CostEstimate {
        val aiInputCost = (inputTokens / 1000.0) * costPer1kInputTokens
        val aiOutputCost = (outputTokens / 1000.0) * costPer1kOutputTokens
        val ttsCost = (ttsCharacters / 1000.0) * costPer1kTtsChars
        val total = aiInputCost + aiOutputCost + ttsCost
        val estimate = CostEstimate(
            aiCostUsd = aiInputCost + aiOutputCost,
            ttsCostUsd = ttsCost,
            totalUsd = total,
            inputTokens = inputTokens,
            outputTokens = outputTokens,
            ttsCharacters = ttsCharacters
        )
        costEstimateTotalUsd.increment(total)
        log.info("Cost estimate: ai={}USD tts={}USD total={}USD tokens={}+{} chars={} {}",
            "%.4f".format(estimate.aiCostUsd), "%.4f".format(estimate.ttsCostUsd), "%.4f".format(estimate.totalUsd),
            inputTokens, outputTokens, ttsCharacters, context)
        return estimate
    }
}
