package com.araro.api.config

import com.araro.api.ApiVersion
import com.araro.api.exception.ErrorResponse
import com.araro.infrastructure.config.AppProperties
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletResponse
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
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import java.time.Instant

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig(
    private val appProperties: AppProperties,
    private val requestTracingFilter: RequestTracingFilter,
    private val rateLimitingFilter: RateLimitingFilter,
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val storyGenerationRateLimitFilter: StoryGenerationRateLimitFilter,
    private val objectMapper: ObjectMapper
) {

    private fun writeJsonError(response: HttpServletResponse, status: Int, message: String) {
        response.status = status
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = "UTF-8"
        val body = ErrorResponse(
            message = message,
            status = status,
            traceId = null,
            timestamp = Instant.now().toString()
        )
        objectMapper.writeValue(response.outputStream, body)
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val config = CorsConfiguration().apply {
            allowCredentials = true
            val origins = appProperties.cors.allowedOrigins.trim()
            if (origins.isNotEmpty() && origins != "*") {
                allowedOrigins = origins.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            } else {
                allowedOriginPatterns = listOf("*")
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
            .sessionManagement {
                it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/actuator/health/**", "/actuator/info", "/actuator/prometheus").permitAll()
                    .requestMatchers("$v1/health").permitAll()
                    .requestMatchers("$v1/auth/register", "$v1/auth/login", "$v1/auth/refresh", "$v1/auth/otp/send", "$v1/auth/otp/verify", "$v1/auth/passwordless", "$v1/auth/passwordless/verify").permitAll()
                    .requestMatchers("$v1/dev/**").permitAll()
                    .requestMatchers("$v1/soundscapes").permitAll()
                    .requestMatchers("$v1/webhooks/**").permitAll()
                    .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                    // Audio files: permit when CDN disabled for direct playback (ExoPlayer does not send auth headers)
                    .requestMatchers("/audio/**").permitAll()
                    // Cover images: proxy to S3; permit for unauthenticated img loads
                    .requestMatchers("$v1/covers/**").permitAll()
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
