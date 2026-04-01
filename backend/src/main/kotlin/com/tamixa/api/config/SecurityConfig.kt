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
import org.slf4j.LoggerFactory
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
    private val log = LoggerFactory.getLogger(SecurityConfig::class.java)

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
            val origins = appProperties.cors.allowedOrigins.trim()
            val isDev = environment.activeProfiles.contains("dev")
            if (origins.isNotEmpty() && origins != "*") {
                allowedOrigins = origins.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            } else if (isDev) {
                // Dev: allow admin app (Next.js default port) so login works from browser
                allowedOrigins = listOf(
                    "http://localhost:3000",
                    "http://127.0.0.1:3000",
                    "http://localhost:3001",
                    "http://127.0.0.1:3001"
                )
            } else if (environment.activeProfiles.any { it.equals("staging", ignoreCase = true) } &&
                (origins.isEmpty() || origins == "*")
            ) {
                // Staging (e.g. Railway): default YAML uses *; explicit origins still required for prod.
                // Patterns cover local UIs and typical Railway HTTPS hostnames until CORS_ALLOWED_ORIGINS is set.
                allowedOriginPatterns = listOf(
                    "http://localhost:*",
                    "http://127.0.0.1:*",
                    "https://*.up.railway.app",
                    "https://*.railway.app"
                )
                log.warn(
                    "CORS: staging uses default origin patterns (localhost + Railway). " +
                        "Set CORS_ALLOWED_ORIGINS to comma-separated HTTPS origins for stricter control."
                )
            } else {
                // Production: ALLOWED_ORIGINS must be explicitly configured; fail-fast to prevent open CORS
                throw IllegalStateException(
                    "ALLOWED_ORIGINS must be set in production. Set environment variable CORS_ALLOWED_ORIGINS " +
                        "(or app.cors.allowed-origins) to a comma-separated list of allowed browser origins (HTTPS), not *."
                )
            }
            allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            allowedHeaders = listOf("*")
            exposedHeaders = listOf("X-Request-Id", "Authorization", "X-RateLimit-Limit", "X-RateLimit-Remaining")
        }
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", config)
        return source
    }

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
                auth
                    // Actuator: only health (and liveness/readiness) public for load balancers; rest require auth in prod
                    .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                    .requestMatchers("/actuator/**").authenticated()
                    .requestMatchers("$v1/health").permitAll()
                    .requestMatchers("$v1/auth/register", "$v1/auth/login", "$v1/auth/refresh", "$v1/auth/otp/send", "$v1/auth/otp/verify", "$v1/auth/passwordless", "$v1/auth/passwordless/verify").permitAll()
                    .requestMatchers("$v1/dev/**").permitAll()
                    .requestMatchers("$v1/webhooks/**").permitAll()
                    // Swagger and api-docs: require authenticated in prod to reduce reconnaissance
                    .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").authenticated()
                    // Audio files: permit when CDN disabled for direct playback (ExoPlayer does not send auth headers)
                    .requestMatchers("/audio/**").permitAll()
                    // Cover images: proxy to S3; permit for unauthenticated img loads
                    .requestMatchers("$v1/covers/**").permitAll()
                    // Admin voice test: explicit allow for any admin role (avoids 403 when permission bean is strict)
                    .requestMatchers(
                        AntPathRequestMatcher.antMatcher(HttpMethod.GET, "$v1/admin/parents/*/voice"),
                        AntPathRequestMatcher.antMatcher(HttpMethod.POST, "$v1/admin/parents/*/voice/upload")
                    ).hasAnyRole("ADMIN", "SUPER_ADMIN", "CONTENT_MANAGER", "REVENUE_ANALYST", "SUPPORT")
                    // Reserved public control-plane prefix; use `/api/v1/admin/ai-control-plane/` (RBAC) for operations.
                    .requestMatchers("/api/control-plane/**").denyAll()
                    .anyRequest().authenticated()
            }
            .exceptionHandling { ex ->
                ex.authenticationEntryPoint { request, response, authException ->
                    writeJsonError(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized. Please log in.")
                }
                ex.accessDeniedHandler { request, response, accessDeniedException ->
                    writeJsonError(response, HttpServletResponse.SC_FORBIDDEN, "Access denied. Please log in with a valid account.")
                }
            }
            .addFilterBefore(storyGenerationRateLimitFilter, UsernamePasswordAuthenticationFilter::class.java)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
            .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter::class.java)
            .addFilterBefore(requestTracingFilter, UsernamePasswordAuthenticationFilter::class.java)
            .build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()
}
