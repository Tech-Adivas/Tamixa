package com.tamixa.api.config

import com.tamixa.api.ApiVersion
import com.tamixa.infrastructure.config.AppProperties
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.ObjectProvider
import org.springframework.core.env.Environment
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter
import org.springframework.security.web.util.matcher.AntPathRequestMatcher
import org.springframework.http.HttpMethod
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.slf4j.MDC
import java.time.Instant

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig(
    private val appProperties: AppProperties,
    private val environment: Environment,
    private val requestTracingFilter: RequestTracingFilter,
    redisRateLimitingFilter: ObjectProvider<RedisRateLimitingFilter>,
    inMemoryRateLimitingFilter: ObjectProvider<RateLimitingFilter>,
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val storyGenerationRateLimitFilter: StoryGenerationRateLimitFilter,
    private val objectMapper: ObjectMapper
) {

    /** Exactly one of [RedisRateLimitingFilter] / [RateLimitingFilter] is registered (see @ConditionalOnProperty). */
    private val rateLimitingFilter: OncePerRequestFilter =
        redisRateLimitingFilter.getIfAvailable()
            ?: inMemoryRateLimitingFilter.getIfAvailable()
            ?: error("No API rate limit filter: enable app.rate-limit or fix configuration")

    private fun writeJsonError(response: HttpServletResponse, status: Int, message: String) {
        response.status = status
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = "UTF-8"
        val traceId = MDC.get(RequestTracingFilter.TRACE_ID_MDC_KEY).orEmpty()
        val body = mapOf(
            "message" to message,
            "status" to status,
            "traceId" to traceId,
            "timestamp" to Instant.now().toString()
        )
        objectMapper.writeValue(response.outputStream, body)
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val config = CorsConfiguration().apply {
            allowCredentials = true
            val originsRaw = appProperties.cors.allowedOrigins.trim()
            val patternsRaw = appProperties.cors.allowedOriginPatterns
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            val explicitOrigins = originsRaw.isNotEmpty() && originsRaw != "*"
            if (explicitOrigins) {
                allowedOrigins = originsRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            }
            if (patternsRaw.isNotEmpty()) {
                allowedOriginPatterns = patternsRaw
            }
            val hasCorsPolicy = explicitOrigins || patternsRaw.isNotEmpty()
            if (!hasCorsPolicy) {
                if (environment.activeProfiles.contains("dev")) {
                    // Last resort if dev profile is active but cors was not bound from application-dev.yml
                    allowedOrigins = listOf(
                        "http://localhost:3000",
                        "http://127.0.0.1:3000",
                        "http://localhost:3001",
                        "http://127.0.0.1:3001"
                    )
                } else {
                    throw IllegalStateException(
                        "CORS is not configured for this environment. Set CORS_ALLOWED_ORIGINS (comma-separated " +
                            "HTTPS origins, not *) and/or CORS_ALLOWED_ORIGIN_PATTERNS, or activate a profile that " +
                            "defines app.cors in application-<profile>.yml (e.g. dev, staging)."
                    )
                }
            }
            allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            allowedHeaders = listOf("*")
            exposedHeaders =
                listOf(
                    "X-Request-Id",
                    "X-Correlation-Id",
                    "Authorization",
                    "X-RateLimit-Limit",
                    "X-RateLimit-Remaining",
                )
        }
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", config)
        return source
    }

    private fun devProfileActive(): Boolean =
        environment.activeProfiles.any { it.equals("dev", ignoreCase = true) }

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        val v1 = ApiVersion.V1
        return http
            .cors { it.configurationSource(corsConfigurationSource()) }
            .csrf { it.disable() }
            .headers { headers ->
                headers.frameOptions { it.deny() }
                headers.referrerPolicy { it.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN) }
            }
            .sessionManagement {
                it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .authorizeHttpRequests { auth ->
                // Actuator: only health (and liveness/readiness) public for load balancers; rest require auth in prod
                auth.requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                auth.requestMatchers("/actuator/**").authenticated()
                auth.requestMatchers("$v1/health").permitAll()
                auth.requestMatchers(
                    "$v1/auth/register",
                    "$v1/auth/login",
                    "$v1/auth/refresh",
                    "$v1/auth/otp/send",
                    "$v1/auth/otp/verify",
                    "$v1/auth/passwordless",
                    "$v1/auth/passwordless/verify"
                ).permitAll()
                // Dev controllers are @Profile("dev"); deny /dev/** in non-dev so misconfiguration cannot expose tooling.
                if (devProfileActive()) {
                    auth.requestMatchers("$v1/dev/**").permitAll()
                } else {
                    auth.requestMatchers("$v1/dev/**").denyAll()
                }
                auth.requestMatchers("$v1/webhooks/**").permitAll()
                // Swagger and api-docs: require authenticated in prod to reduce reconnaissance
                auth.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").authenticated()
                // Audio files: permit when CDN disabled for direct playback (ExoPlayer does not send auth headers)
                auth.requestMatchers("/audio/**").permitAll()
                // Cover images: proxy to S3; permit for unauthenticated img loads
                auth.requestMatchers("$v1/covers/**").permitAll()
                // Admin voice test: explicit allow for any admin role (avoids 403 when permission bean is strict)
                auth.requestMatchers(
                    AntPathRequestMatcher.antMatcher(HttpMethod.GET, "$v1/admin/parents/*/voice"),
                    AntPathRequestMatcher.antMatcher(HttpMethod.POST, "$v1/admin/parents/*/voice/upload")
                ).hasAnyRole("ADMIN", "SUPER_ADMIN", "CONTENT_MANAGER", "REVENUE_ANALYST", "SUPPORT")
                // Reserved public control-plane prefix; use `/api/v1/admin/ai-control-plane/` (RBAC) for operations.
                auth.requestMatchers("/api/control-plane/**").denyAll()
                auth.anyRequest().authenticated()
            }
            .exceptionHandling { ex ->
                ex.authenticationEntryPoint { request, response, authException ->
                    writeJsonError(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized. Please log in.")
                }
                ex.accessDeniedHandler { request, response, accessDeniedException ->
                    writeJsonError(response, HttpServletResponse.SC_FORBIDDEN, "Access denied. Please log in with a valid account.")
                }
            }
            // Outermost first: MDC traceId must exist before rate limit / JWT / story-gen filters (they log traceId).
            .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter::class.java)
            .addFilterBefore(jwtAuthenticationFilter, rateLimitingFilter::class.java)
            .addFilterBefore(storyGenerationRateLimitFilter, jwtAuthenticationFilter::class.java)
            .addFilterBefore(requestTracingFilter, storyGenerationRateLimitFilter::class.java)
            .build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()
}
