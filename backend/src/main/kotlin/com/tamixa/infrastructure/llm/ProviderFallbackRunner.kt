package com.tamixa.infrastructure.llm

import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory

/** Thrown when both the primary and the backup provider failed. Primary error is attached as suppressed. */
class AllProvidersFailedException(
    feature: String,
    primaryName: String,
    primaryError: Throwable,
    fallbackName: String,
    fallbackError: Throwable,
) : IllegalStateException(
    "$feature failed on $primaryName (${ProviderFallbackPolicy.reason(primaryError)}) " +
        "and on backup $fallbackName (${ProviderFallbackPolicy.reason(fallbackError)})",
    fallbackError,
) {
    init {
        addSuppressed(primaryError)
    }
}

/**
 * Runs a call on the primary provider and, when [ProviderFallbackPolicy] allows, repeats it on the backup.
 * Records `tamixa.ai.provider.fallback{feature,from,to,outcome}` so fallbacks show up in metrics.
 */
class ProviderFallbackRunner(
    private val feature: String,
    private val primaryName: String,
    private val fallbackName: String,
    private val meterRegistry: MeterRegistry? = null,
) {
    private val log = LoggerFactory.getLogger(ProviderFallbackRunner::class.java)

    /**
     * @param isUsable result check — e.g. reject blank text or null audio so an "empty success" also falls back.
     */
    fun <T> run(
        operation: String,
        isUsable: (T) -> Boolean = { true },
        primary: () -> T,
        fallback: () -> T,
    ): T {
        val primaryError: Throwable = try {
            val result = primary()
            if (isUsable(result)) return result
            IllegalStateException("$primaryName returned an empty result")
        } catch (e: Exception) {
            if (!ProviderFallbackPolicy.shouldFallBack(e)) throw e
            e
        }
        log.warn(
            "AI fallback: {} {} failed on {} ({}); trying {}",
            feature, operation, primaryName, ProviderFallbackPolicy.reason(primaryError), fallbackName,
        )
        return try {
            val result = fallback()
            if (!isUsable(result)) throw IllegalStateException("$fallbackName returned an empty result")
            count("recovered")
            log.info("AI fallback: {} {} recovered on {}", feature, operation, fallbackName)
            result
        } catch (e: Exception) {
            count("failed")
            throw AllProvidersFailedException(feature, primaryName, primaryError, fallbackName, e)
        }
    }

    private fun count(outcome: String) {
        try {
            meterRegistry?.counter(
                "tamixa.ai.provider.fallback",
                "feature", feature, "from", primaryName, "to", fallbackName, "outcome", outcome,
            )?.increment()
        } catch (_: Exception) {
            // metrics must never break the request
        }
    }
}
