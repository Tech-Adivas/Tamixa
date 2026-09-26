package com.tamixa.infrastructure.jwt

import com.tamixa.application.port.JwtPort
import com.tamixa.application.port.TokenClaims
import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.MalformedJwtException
import io.jsonwebtoken.security.Keys
import io.jsonwebtoken.security.SignatureException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.Date
import javax.crypto.SecretKey

@Service
class JwtService(
    @Value("\${app.jwt.secret}") private val secret: String,
    @Value("\${app.jwt.access-expiration-ms}") private val accessExpirationMs: Long,
    @Value("\${app.jwt.refresh-expiration-ms}") private val refreshExpirationMs: Long
) : JwtPort {

    private val log = LoggerFactory.getLogger(javaClass)

    private val key: SecretKey by lazy {
        Keys.hmacShaKeyFor(secret.encodeToByteArray())
    }

    override fun generateAccessToken(email: String, role: String): String {
        val now = Date()
        val expiry = Date(now.time + accessExpirationMs)
        return Jwts.builder()
            .subject(email)
            .claim(CLAIM_ROLE, role)
            .claim(CLAIM_TYPE, TYPE_ACCESS)
            .issuedAt(now)
            .expiration(expiry)
            .signWith(key)
            .compact()
    }

    override fun generateRefreshToken(email: String, role: String): String {
        val now = Date()
        val expiry = Date(now.time + refreshExpirationMs)
        return Jwts.builder()
            .subject(email)
            .claim(CLAIM_ROLE, role)
            .claim(CLAIM_TYPE, TYPE_REFRESH)
            .issuedAt(now)
            .expiration(expiry)
            .signWith(key)
            .compact()
    }

    override fun validateAccessToken(token: String): TokenClaims? {
        return try {
            val claims = parseToken(token)
            if (claims[CLAIM_TYPE] != TYPE_ACCESS) return null
            val role = (claims[CLAIM_ROLE] as? String)?.trim()?.takeIf { it.isNotEmpty() } ?: run {
                log.debug("Access token missing or invalid role claim")
                return null
            }
            val issuedAt = claims.issuedAt?.toInstant() ?: run {
                log.debug("Access token missing issuedAt claim")
                return null
            }
            TokenClaims(email = claims.subject, role = role, issuedAt = issuedAt)
        } catch (e: ExpiredJwtException) {
            log.debug("Access token expired")
            null
        } catch (e: MalformedJwtException) {
            log.debug("Invalid access token format")
            null
        } catch (e: SignatureException) {
            log.debug("Invalid access token signature")
            null
        } catch (e: ClassCastException) {
            log.debug("Access token role claim has wrong type: {}", e.message)
            null
        }
    }

    override fun getAccessExpirationSeconds(): Long = accessExpirationMs / 1000

    override fun validateRefreshToken(token: String): TokenClaims? {
        return try {
            val claims = parseToken(token)
            if (claims[CLAIM_TYPE] != TYPE_REFRESH) return null
            val role = (claims[CLAIM_ROLE] as? String)?.trim()?.takeIf { it.isNotEmpty() }
                ?: run { log.debug("Refresh token missing role claim — rejecting"); return null }
            val issuedAt = claims.issuedAt?.toInstant() ?: run {
                log.debug("Refresh token missing issuedAt claim")
                return null
            }
            TokenClaims(email = claims.subject, role = role, issuedAt = issuedAt)
        } catch (e: ExpiredJwtException) {
            log.debug("Refresh token expired")
            null
        } catch (e: MalformedJwtException) {
            log.debug("Invalid refresh token format")
            null
        } catch (e: SignatureException) {
            log.debug("Invalid refresh token signature")
            null
        }
    }

    private fun parseToken(token: String): Claims {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload
    }

    override fun getTokenExpiration(token: String): Instant? {
        return try {
            val claims = parseToken(token)
            claims.expiration?.toInstant()
        } catch (e: Exception) {
            log.debug("Failed to extract token expiration: {}", e.message)
            null
        }
    }

    companion object {
        private const val CLAIM_ROLE = "role"
        private const val CLAIM_TYPE = "type"
        private const val TYPE_ACCESS = "access"
        private const val TYPE_REFRESH = "refresh"
    }
}
