# Observability Enhancement Implementation Guide

This guide provides step-by-step instructions for implementing the recommended observability enhancements.

---

## 1. Add APM/Distributed Tracing (Datadog or Jaeger)

### Option A: Datadog APM

**Estimated Effort:** 4 hours

#### Step 1: Add Dependencies

```kotlin
// backend/build.gradle.kts
dependencies {
    implementation("io.micrometer:micrometer-registry-datadog")
    implementation("io.micrometer:micrometer-tracing-bridge-brave")
    implementation("io.zipkin.reporter2:zipkin-reporter-brave")
}
```

#### Step 2: Configure Datadog

```yaml
# backend/src/main/resources/application-prod.yml
management:
  metrics:
    export:
      datadog:
        enabled: true
        api-key: ${DATADOG_API_KEY}
        application-key: ${DATADOG_APP_KEY}
        uri: https://api.datadoghq.com
        step: 1m
  tracing:
    sampling:
      probability: 0.1  # 10% sampling in prod
```

#### Step 3: Add Environment Variables

```bash
# .env.example
# Datadog APM
DATADOG_API_KEY=           # From Datadog dashboard
DATADOG_APP_KEY=           # From Datadog dashboard
```

#### Step 4: Add Custom Spans

```kotlin
// backend/src/main/kotlin/com/tamixa/application/story/StoryService.kt
import io.micrometer.tracing.Tracer
import io.micrometer.tracing.annotation.NewSpan
import io.micrometer.tracing.annotation.SpanTag

@Service
class StoryService(
    private val tracer: Tracer
) {
    @NewSpan("story.generation")
    fun generateStory(
        @SpanTag("parent.id") parentId: Long,
        @SpanTag("theme") theme: String
    ): Story {
        val span = tracer.currentSpan()
        span?.tag("child.age", child.age.toString())
        
        // Story generation logic
        
        return story
    }
}
```

#### Step 5: Verify

```bash
# Start application
./gradlew :backend:bootRun

# Generate some traffic
curl -X POST http://localhost:8080/api/v1/stories \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"theme":"adventure","childId":1}'

# Check Datadog dashboard
# Navigate to APM > Traces
# You should see traces for story.generation
```

---

### Option B: Jaeger (Open Source)

**Estimated Effort:** 6 hours

#### Step 1: Deploy Jaeger

```bash
# Using Docker
docker run -d --name jaeger \
  -e COLLECTOR_ZIPKIN_HOST_PORT=:9411 \
  -p 5775:5775/udp \
  -p 6831:6831/udp \
  -p 6832:6832/udp \
  -p 5778:5778 \
  -p 16686:16686 \
  -p 14268:14268 \
  -p 14250:14250 \
  -p 9411:9411 \
  jaegertracing/all-in-one:latest
```

#### Step 2: Add Dependencies

```kotlin
// backend/build.gradle.kts
dependencies {
    implementation("io.micrometer:micrometer-tracing-bridge-brave")
    implementation("io.zipkin.reporter2:zipkin-reporter-brave")
    implementation("io.opentracing.contrib:opentracing-spring-jaeger-web-starter:3.3.1")
}
```

#### Step 3: Configure Jaeger

```yaml
# backend/src/main/resources/application.yml
management:
  tracing:
    sampling:
      probability: ${TRACING_SAMPLE_RATE:0.1}
  zipkin:
    tracing:
      endpoint: ${JAEGER_ENDPOINT:http://localhost:9411/api/v2/spans}

opentracing:
  jaeger:
    service-name: tamixa-backend
    udp-sender:
      host: ${JAEGER_AGENT_HOST:localhost}
      port: ${JAEGER_AGENT_PORT:6831}
```

#### Step 4: Verify

```bash
# Access Jaeger UI
open http://localhost:16686

# Generate traffic and check traces
```

---

## 2. Add Error Tracking (Sentry)

**Estimated Effort:** 4 hours

#### Step 1: Create Sentry Project

1. Sign up at https://sentry.io
2. Create new project: "Tamixa Backend"
3. Copy DSN from project settings

#### Step 2: Add Dependencies

```kotlin
// backend/build.gradle.kts
dependencies {
    implementation("io.sentry:sentry-spring-boot-starter-jakarta:7.0.0")
    implementation("io.sentry:sentry-logback:7.0.0")
}
```

#### Step 3: Configure Sentry

```yaml
# backend/src/main/resources/application.yml
sentry:
  dsn: ${SENTRY_DSN:}
  environment: ${SPRING_PROFILES_ACTIVE:dev}
  traces-sample-rate: 0.1
  enable-tracing: true
  send-default-pii: false  # Don't send PII
  before-send: com.tamixa.infrastructure.sentry.SentryBeforeSendCallback
```

#### Step 4: Add PII Scrubbing

```kotlin
// backend/src/main/kotlin/com/tamixa/infrastructure/sentry/SentryBeforeSendCallback.kt
package com.tamixa.infrastructure.sentry

import io.sentry.SentryEvent
import io.sentry.SentryOptions
import io.sentry.Hint
import com.tamixa.infrastructure.logging.PiiMask
import org.springframework.stereotype.Component

@Component
class SentryBeforeSendCallback : SentryOptions.BeforeSendCallback {
    
    override fun execute(event: SentryEvent, hint: Hint): SentryEvent? {
        // Scrub PII from event
        event.contexts?.forEach { (key, context) ->
            context.forEach { (k, v) ->
                if (k.contains("email", ignoreCase = true) && v is String) {
                    context[k] = PiiMask.maskEmail(v)
                }
                if (k.contains("phone", ignoreCase = true) && v is String) {
                    context[k] = PiiMask.maskPhone(v)
                }
            }
        }
        
        // Scrub PII from breadcrumbs
        event.breadcrumbs?.forEach { breadcrumb ->
            breadcrumb.data?.forEach { (k, v) ->
                if (k.contains("email", ignoreCase = true) && v is String) {
                    breadcrumb.data[k] = PiiMask.maskEmail(v)
                }
            }
        }
        
        return event
    }
}
```

#### Step 5: Add Custom Context

```kotlin
// backend/src/main/kotlin/com/tamixa/api/config/SentryContextFilter.kt
package com.tamixa.api.config

import io.sentry.Sentry
import io.sentry.protocol.User
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class SentryContextFilter : OncePerRequestFilter() {
    
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val auth = SecurityContextHolder.getContext().authentication
        if (auth != null && auth.isAuthenticated) {
            Sentry.setUser(User().apply {
                id = auth.name  // Email (will be masked by BeforeSendCallback)
            })
        }
        
        Sentry.setTag("request.path", request.requestURI)
        Sentry.setTag("request.method", request.method)
        
        try {
            filterChain.doFilter(request, response)
        } finally {
            Sentry.clearBreadcrumbs()
        }
    }
}
```

#### Step 6: Add Environment Variables

```bash
# .env.example
# Sentry Error Tracking
SENTRY_DSN=                # From Sentry project settings
```

#### Step 7: Test Error Tracking

```kotlin
// backend/src/main/kotlin/com/tamixa/api/controller/HealthController.kt
@GetMapping("/test-sentry")
fun testSentry(): ResponseEntity<*> {
    try {
        throw RuntimeException("Test Sentry error tracking")
    } catch (e: Exception) {
        Sentry.captureException(e)
        throw e
    }
}
```

```bash
# Trigger test error
curl http://localhost:8080/api/v1/health/test-sentry

# Check Sentry dashboard for error
```

---

## 3. Enhanced Logging with Correlation

**Already Implemented** ✅

The application already has excellent correlation via `RequestTracingFilter`:

```kotlin
// backend/src/main/kotlin/com/tamixa/api/config/RequestTracingFilter.kt
class RequestTracingFilter : OncePerRequestFilter() {
    override fun doFilterInternal(...) {
        val traceId = UUID.randomUUID().toString()
        MDC.put(TRACE_ID_MDC_KEY, traceId)
        response.setHeader("X-Request-Id", traceId)
        // ...
    }
}
```

### Enhancement: Add User Context to Logs

```kotlin
// backend/src/main/kotlin/com/tamixa/api/config/UserContextMdcFilter.kt
package com.tamixa.api.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import com.tamixa.infrastructure.logging.PiiMask

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 3)
class UserContextMdcFilter : OncePerRequestFilter() {
    
    companion object {
        const val USER_ID_MDC_KEY = "userId"
        const val USER_ROLE_MDC_KEY = "userRole"
    }
    
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            val auth = SecurityContextHolder.getContext().authentication
            if (auth != null && auth.isAuthenticated) {
                MDC.put(USER_ID_MDC_KEY, PiiMask.maskEmail(auth.name))
                MDC.put(USER_ROLE_MDC_KEY, auth.authorities.firstOrNull()?.authority ?: "UNKNOWN")
            }
            filterChain.doFilter(request, response)
        } finally {
            MDC.remove(USER_ID_MDC_KEY)
            MDC.remove(USER_ROLE_MDC_KEY)
        }
    }
}
```

Update logback configuration:

```xml
<!-- backend/src/main/resources/logback-spring.xml -->
<springProfile name="prod,staging">
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <includeMdcKeyName>traceId</includeMdcKeyName>
            <includeMdcKeyName>masterStoryId</includeMdcKeyName>
            <includeMdcKeyName>language</includeMdcKeyName>
            <includeMdcKeyName>userId</includeMdcKeyName>
            <includeMdcKeyName>userRole</includeMdcKeyName>
        </encoder>
    </appender>
</springProfile>
```

---

## 4. Alerting Setup

### Datadog Alerts

```yaml
# datadog-alerts.yml
alerts:
  - name: "High Error Rate"
    query: "avg(last_5m):sum:trace.servlet.request.errors{env:production} > 10"
    message: |
      High error rate detected in production
      @slack-tamixa-alerts
      @pagerduty-oncall
    
  - name: "Slow Story Generation"
    query: "avg(last_10m):avg:story.generation.latency{env:production} > 30000"
    message: |
      Story generation is slow (>30s average)
      Check OpenAI API status
      @slack-tamixa-alerts
    
  - name: "Database Connection Pool Exhausted"
    query: "avg(last_5m):avg:hikaricp.connections.active{env:production} > 18"
    message: |
      Database connection pool near capacity (18/20)
      Consider scaling or investigating slow queries
      @slack-tamixa-alerts
```

### Sentry Alerts

1. Navigate to Sentry project settings
2. Go to Alerts > Create Alert Rule
3. Configure:
   - **Trigger:** Error count > 10 in 5 minutes
   - **Action:** Send to Slack #tamixa-alerts
   - **Action:** Create PagerDuty incident

### Prometheus Alerts (Admin Story Update)

Use this when `admin_story_update_latency` is enabled with histogram + SLO buckets in `application.yml`.

```yaml
# prometheus-rules-admin-story-update.yml
groups:
  - name: tamixa-admin-story-update
    rules:
      - alert: AdminStoryUpdateP99High
        expr: |
          histogram_quantile(
            0.99,
            sum(rate(admin_story_update_latency_seconds_bucket{scope="controller",outcome="success"}[10m]))
            by (le, status)
          ) > 20
        for: 10m
        labels:
          severity: warning
          service: tamixa-backend
        annotations:
          summary: "Admin story update p99 is high"
          description: "p99 > 20s for status={{ $labels.status }} over 10m (controller scope)."

      - alert: AdminStoryUpdateErrorRateHigh
        expr: |
          (
            sum(rate(admin_story_update_latency_seconds_count{scope="controller",outcome="error"}[10m]))
            /
            clamp_min(sum(rate(admin_story_update_latency_seconds_count{scope="controller"}[10m])), 1e-9)
          ) > 0.05
        for: 10m
        labels:
          severity: critical
          service: tamixa-backend
        annotations:
          summary: "Admin story update error rate is high"
          description: "Error ratio > 5% for PUT /api/v1/admin/stories/{id} over 10m."
```

Useful panel queries:

```promql
# p95 split by scope/status
histogram_quantile(
  0.95,
  sum(rate(admin_story_update_latency_seconds_bucket[5m])) by (le, scope, status)
)

# p99 split by scope/status
histogram_quantile(
  0.99,
  sum(rate(admin_story_update_latency_seconds_bucket[5m])) by (le, scope, status)
)
```

### Prometheus Alerts (Translation Tamil-Leak Recovery)

Use this for translation quality monitoring when `translation_tamil_leak_recovery_total` is emitted by `OpenAITranslationClient`.

```yaml
# prometheus-rules-translation-recovery.yml
groups:
  - name: tamixa-translation-quality
    rules:
      - alert: TranslationTamilLeakRecoveryFailureRatioHigh
        expr: |
          (
            sum(rate(translation_tamil_leak_recovery_total{outcome="failure"}[15m])) by (target, field)
            /
            clamp_min(sum(rate(translation_tamil_leak_recovery_total[15m])) by (target, field), 1e-9)
          ) > 0.20
        for: 15m
        labels:
          severity: warning
          service: tamixa-backend
        annotations:
          summary: "High Tamil-leak recovery failures in translation"
          description: "Recovery failure ratio > 20% for target={{ $labels.target }}, field={{ $labels.field }} over 15m."

      - alert: TranslationTamilLeakRecoveryFailureBurst
        expr: |
          sum(increase(translation_tamil_leak_recovery_total{outcome="failure"}[15m])) by (target) >= 10
        for: 5m
        labels:
          severity: warning
          service: tamixa-backend
        annotations:
          summary: "Burst of Tamil-leak recovery failures"
          description: ">= 10 failed recoveries in 15m for target={{ $labels.target }}."
```

Useful panel queries:

```promql
# Failure ratio by target/field (0..1)
(
  sum(rate(translation_tamil_leak_recovery_total{outcome="failure"}[10m])) by (target, field)
  /
  clamp_min(sum(rate(translation_tamil_leak_recovery_total[10m])) by (target, field), 1e-9)
)

# Recovery success count by target
sum(rate(translation_tamil_leak_recovery_total{outcome="success"}[10m])) by (target)

# Recovery failure count by target
sum(rate(translation_tamil_leak_recovery_total{outcome="failure"}[10m])) by (target)
```

---

## 5. Monitoring Dashboard

### Grafana Dashboard (JSON)

```json
{
  "dashboard": {
    "title": "Tamixa Backend Monitoring",
    "panels": [
      {
        "title": "Request Rate",
        "targets": [
          {
            "expr": "rate(http_server_requests_seconds_count[5m])"
          }
        ]
      },
      {
        "title": "Error Rate",
        "targets": [
          {
            "expr": "rate(http_server_requests_seconds_count{status=~\"5..\"}[5m])"
          }
        ]
      },
      {
        "title": "Story Generation Latency (p95)",
        "targets": [
          {
            "expr": "histogram_quantile(0.95, rate(story_generation_latency_seconds_bucket[5m]))"
          }
        ]
      },
      {
        "title": "Database Connection Pool",
        "targets": [
          {
            "expr": "hikaricp_connections_active"
          },
          {
            "expr": "hikaricp_connections_max"
          }
        ]
      },
      {
        "title": "JVM Memory Usage",
        "targets": [
          {
            "expr": "jvm_memory_used_bytes{area=\"heap\"}"
          }
        ]
      },
      {
        "title": "Cache Hit Rate",
        "targets": [
          {
            "expr": "rate(story_cache_hits_total[5m]) / (rate(story_cache_hits_total[5m]) + rate(story_cache_misses_total[5m]))"
          }
        ]
      }
    ]
  }
}
```

---

## Testing Observability

### 1. Generate Load

```bash
# Install k6
brew install k6

# Run load test
k6 run - <<EOF
import http from 'k6/http';
import { check, sleep } from 'k6';

export let options = {
  vus: 10,
  duration: '30s',
};

export default function () {
  let res = http.get('http://localhost:8080/api/v1/health');
  check(res, { 'status is 200': (r) => r.status === 200 });
  sleep(1);
}
EOF
```

### 2. Verify Traces

- **Datadog:** Navigate to APM > Traces, filter by service:tamixa-backend
- **Jaeger:** Open http://localhost:16686, search for traces

### 3. Verify Errors

```bash
# Trigger an error
curl -X POST http://localhost:8080/api/v1/stories \
  -H "Authorization: Bearer invalid_token"

# Check Sentry dashboard for error
```

### 4. Verify Metrics

```bash
# Check Prometheus metrics
curl http://localhost:8080/actuator/prometheus | grep story_generation

# Expected output:
# story_generation_latency_seconds_count 42
# story_generation_latency_seconds_sum 1234.56
```

---

## Rollout Plan

### Phase 1: Staging (Week 1)
1. Deploy APM/tracing to staging
2. Deploy Sentry to staging
3. Verify data collection
4. Tune sampling rates

### Phase 2: Production Canary (Week 2)
1. Deploy to 10% of production traffic
2. Monitor for performance impact
3. Verify data quality
4. Adjust configuration

### Phase 3: Production Full Rollout (Week 3)
1. Deploy to 100% of production
2. Set up alerts
3. Create dashboards
4. Train team on new tools

---

## Cost Estimates

| Tool | Tier | Monthly Cost | Notes |
|------|------|--------------|-------|
| **Datadog APM** | Pro | $31/host | 3 hosts = $93/month |
| **Sentry** | Business | $26/month | Up to 50k events |
| **Jaeger** | Self-hosted | $0 | Infrastructure costs only |
| **Grafana Cloud** | Free | $0 | Up to 10k series |

**Recommended:** Datadog APM + Sentry = $119/month

---

## Success Metrics

After implementation, you should be able to:

- ✅ Trace a request from API gateway to database
- ✅ Identify slow database queries
- ✅ Correlate errors across services
- ✅ Alert on anomalies within 5 minutes
- ✅ Debug production issues without SSH access
- ✅ Measure p95/p99 latency for all endpoints
- ✅ Track error rates by endpoint and user role

---

## Support

- **Datadog:** https://docs.datadoghq.com/tracing/
- **Sentry:** https://docs.sentry.io/platforms/java/guides/spring-boot/
- **Jaeger:** https://www.jaegertracing.io/docs/
- **Grafana:** https://grafana.com/docs/grafana/latest/
