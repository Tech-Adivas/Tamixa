package com.tamixa.api.config

import com.tamixa.application.port.JwtPort
import com.tamixa.application.port.TokenRevocationPort
import com.tamixa.infrastructure.logging.PiiMask
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtPort: JwtPort,
    private val tokenRevocationPort: TokenRevocationPort
) : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            val token = extractToken(request)
            if (token != null) {
                // Check token-level revocation first (fast path)
                if (tokenRevocationPort.isRevoked(token)) {
                    log.debug("Token is revoked")
                    filterChain.doFilter(request, response)
                    return
                }

                val claims = jwtPort.validateAccessToken(token)
                if (claims != null) {
                    // Check user-level revocation (e.g., account deleted, password changed)
                    if (tokenRevocationPort.isUserRevoked(claims.email, claims.issuedAt)) {
                        log.debug("User sessions revoked for email={}", PiiMask.maskEmail(claims.email))
                        filterChain.doFilter(request, response)
                        return
                    }

                    val authority = SimpleGrantedAuthority("ROLE_${claims.role}")
                    val authentication = UsernamePasswordAuthenticationToken(
                        claims.email,
                        null,
                        listOf(authority)
                    )
                    SecurityContextHolder.getContext().authentication = authentication
                    log.debug("Authenticated user: {}", PiiMask.maskEmail(claims.email))
                }
            }
        } catch (e: Exception) {
            log.debug("JWT validation failed: {}", e.message)
        }
        filterChain.doFilter(request, response)
    }

    private fun extractToken(request: HttpServletRequest): String? {
        val bearer = request.getHeader("Authorization") ?: return null
        return if (bearer.startsWith("Bearer ")) bearer.removePrefix("Bearer ") else null
    }
}
