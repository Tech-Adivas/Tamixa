package com.tamixa.infrastructure.observability;

import com.tamixa.application.story.ModerationLayer;
import io.micrometer.core.instrument.*;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Centralized application metrics for production observability.
 */
@Component
public class ApplicationMetrics {

    private final MeterRegistry registry;
    private final Timer storyGenerationTimer;
    private final Timer voiceProcessingTimer;
    private final Counter cacheHits;
    private final Counter cacheMisses;
    private final Counter openaiTokensTotal;
    private final Counter storyValidationFailures;
    private final Counter fallbackUsageCount;
    private final DistributionSummary tokenUsageDistribution;
    private final Counter hallucinationDetectionCount;
    private final Counter moderationFailuresTotal;
    private final AtomicLong safetyScoreSum = new AtomicLong(0);
    private final AtomicLong safetyScoreCount = new AtomicLong(0);
    private final AtomicLong aiTokenDailyUsage = new AtomicLong(0);

    public ApplicationMetrics(MeterRegistry registry) {
        this.registry = registry;
        this.storyGenerationTimer = registry.timer("story_generation_latency");
        this.voiceProcessingTimer = registry.timer("voice.processing.latency");
        this.cacheHits = registry.counter("story.cache.hits");
        this.cacheMisses = registry.counter("story.cache.misses");
        this.openaiTokensTotal = registry.counter("story.openai.tokens.total");
        this.storyValidationFailures = registry.counter("story.validation.failures");
        this.fallbackUsageCount = registry.counter("fallback_usage_count");
        this.tokenUsageDistribution = registry.summary("story.openai.tokens.usage");
        this.hallucinationDetectionCount = registry.counter("hallucination_detection_rate");
        this.moderationFailuresTotal = registry.counter("moderation_failure_count");

        Gauge.builder("safety_score_average", this, m ->
                m.safetyScoreCount.get() == 0 ? 0.0 : (double) m.safetyScoreSum.get() / m.safetyScoreCount.get())
                .register(registry);
        Gauge.builder("ai_token_daily_usage", aiTokenDailyUsage, v -> (double) v.get())
                .register(registry);
    }

    public <T> T recordStoryGenerationLatency(java.util.function.Supplier<T> block) {
        Timer.Sample sample = Timer.start(registry);
        try {
            return block.get();
        } finally {
            sample.stop(storyGenerationTimer);
        }
    }

    public void recordTokenUsage(int totalTokens) {
        if (totalTokens > 0) {
            openaiTokensTotal.increment(totalTokens);
            tokenUsageDistribution.record(totalTokens);
        }
    }

    public void recordModerationFailure() {
        moderationFailuresTotal.increment();
    }

    public void recordModerationFailure(ModerationLayer layer) {
        moderationFailuresTotal.increment();
        registry.counter("moderation_failure_count", "layer", layer.name()).increment();
    }

    public void recordFallbackUsage() {
        fallbackUsageCount.increment();
    }

    public void recordHallucinationDetection() {
        hallucinationDetectionCount.increment();
    }

    public void recordValidationFailure() {
        storyValidationFailures.increment();
    }

    public void recordSafetyScore(int score) {
        safetyScoreSum.addAndGet(score);
        safetyScoreCount.incrementAndGet();
    }

    public void setAiTokenDailyUsage(long tokens) {
        aiTokenDailyUsage.set(tokens);
    }

    public <T> T recordVoiceProcessingLatency(java.util.function.Supplier<T> block) {
        Timer.Sample sample = Timer.start(registry);
        try {
            return block.get();
        } finally {
            sample.stop(voiceProcessingTimer);
        }
    }

    public void recordCacheHit() {
        cacheHits.increment();
    }

    public void recordCacheMiss() {
        cacheMisses.increment();
    }
}
