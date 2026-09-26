# JWT Revocation Strategy Analysis

**Date:** May 17, 2026  
**Status:** ⚠️ **NOT IMPLEMENTED**

## Executive Summary

The Tamixa backend **does not currently implement JWT token revocation**. The system uses stateless JWT authentication without any mechanism to invalidate tokens before their natural expiration.

---

## Current Implementation

### Authentication Architecture

1. **JWT Generation** (`JwtService.kt`)
   - Access tokens: Short-lived (configurable via `app.jwt.access-expiration-ms`)
   - Refresh tokens: Long-lived (configurable via `app.jwt.refresh-expiration-ms`)
   - Tokens are signed with HMAC-SHA using a secret key
   - Token types distinguished by `type` claim (`access` vs `refresh`)

2. **JWT Validation** (`JwtAuthenticationFilter.kt`)
   - Validates token signature and expiration on every request
   - Extracts claims (email, role) and sets Spring Security context
   - **No revocation check** - only validates signature and expiration

3. **Logout Behavior**
   - **Backend:** No logout endpoint exists in `AuthController.kt`
   - **Mobile:** Client-side only - `TokenStorage.clear()` removes tokens locally
   - **Admin:** Client-side only - clears tokens from browser storage
   - **Web:** Client-side only - clears tokens from local storage

### Security Implications

| Risk | Severity | Description |
|------|----------|-------------|
| **Stolen token remains valid** | 🔴 High | If an access/refresh token is compromised, it remains valid until expiration |
| **No forced logout** | 🔴 High | Cannot revoke sessions when account is compromised or deleted |
| **Account deletion gap** | 🔴 High | Deleted accounts can still authenticate until tokens expire |
| **Password change gap** | 🟡 Medium | Password changes don't invalidate existing sessions |
| **Role change delay** | 🟡 Medium | Role/permission changes take effect only after token refresh |

---

## Compliance & Documentation Gaps

### COMPLIANCE.md References

The compliance document mentions revocation but it's **not implemented**:

> **Step 2:** Revoke all sessions (invalidate refresh tokens; optional blocklist) - Backend

**Status:** ❌ Not implemented

### ARCHITECTURE_AND_FLOWS.md

> **Logout:** Logout is local-only (TokenStorage.clear()). If backend supports token invalidation, add optional logout API call.

**Status:** Backend does NOT support token invalidation

---

## Recommended Implementation Strategy

### Option 1: Redis Token Blacklist (Recommended)

**Pros:**
- Fast lookup (O(1) with Redis)
- Automatic expiration via TTL
- Existing Redis infrastructure in place
- Minimal performance impact

**Cons:**
- Adds Redis dependency to auth flow
- Requires Redis availability for authentication

**Implementation:**

```kotlin
// 1. Create TokenRevocationPort
interface TokenRevocationPort {
    fun revokeToken(token: String, expiresAt: Instant)
    fun isRevoked(token: String): Boolean
    fun revokeAllForUser(email: String)
}

// 2. Redis Adapter
@Service
@ConditionalOnProperty(name = ["app.jwt.revocation.enabled"], havingValue = "true")
class RedisTokenRevocationAdapter(
    private val redisTemplate: RedisTemplate<String, String>
) : TokenRevocationPort {
    
    override fun revokeToken(token: String, expiresAt: Instant) {
        val key = "revoked:token:${hashToken(token)}"
        val ttl = Duration.between(Instant.now(), expiresAt).seconds
        if (ttl > 0) {
            redisTemplate.opsForValue().set(key, "1", ttl, TimeUnit.SECONDS)
        }
    }
    
    override fun isRevoked(token: String): Boolean {
        val key = "revoked:token:${hashToken(token)}"
        return redisTemplate.hasKey(key) == true
    }
    
    override fun revokeAllForUser(email: String) {
        // Store user-level revocation timestamp
        val key = "revoked:user:${email}"
        redisTemplate.opsForValue().set(key, Instant.now().toString())
    }
    
    private fun hashToken(token: String): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(token.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}

// 3. Update JwtAuthenticationFilter
@Component
class JwtAuthenticationFilter(
    private val jwtPort: JwtPort,
    private val tokenRevocationPort: TokenRevocationPort? = null // Optional
) : OncePerRequestFilter() {
    
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            val token = extractToken(request)
            if (token != null) {
                // Check revocation BEFORE validation
                if (tokenRevocationPort?.isRevoked(token) == true) {
                    log.debug("Token is revoked")
                    filterChain.doFilter(request, response)
                    return
                }
                
                val claims = jwtPort.validateAccessToken(token)
                if (claims != null) {
                    // Check user-level revocation
                    if (tokenRevocationPort?.isUserRevoked(claims.email, claims.issuedAt) == true) {
                        log.debug("User sessions revoked")
                        filterChain.doFilter(request, response)
                        return
                    }
                    
                    val authority = SimpleGrantedAuthority("ROLE_${claims.role}")
                    val authentication = UsernamePasswordAuthenticationToken(
                        claims.email, null, listOf(authority)
                    )
                    SecurityContextHolder.getContext().authentication = authentication
                }
            }
        } catch (e: Exception) {
            log.debug("JWT validation failed: {}", e.message)
        }
        filterChain.doFilter(request, response)
    }
}

// 4. Add logout endpoint
@PostMapping("/logout")
fun logout(@RequestHeader("Authorization") authHeader: String): ResponseEntity<Map<String, String>> {
    val token = authHeader.removePrefix("Bearer ").trim()
    val claims = jwtPort.validateAccessToken(token)
    if (claims != null) {
        val expiresAt = jwtPort.getTokenExpiration(token)
        tokenRevocationPort.revokeToken(token, expiresAt)
        log.info("User logged out email={}", PiiMask.maskEmail(claims.email))
    }
    return ResponseEntity.ok(mapOf("message" to "Logged out successfully"))
}

// 5. Update AccountDeletionService
fun deleteAccount(email: String) {
    // ... existing deletion logic ...
    
    // Revoke all sessions
    tokenRevocationPort.revokeAllForUser(email)
    log.info("Revoked all sessions for deleted account email={}", PiiMask.maskEmail(email))
}
```

### Option 2: Database Token Store

**Pros:**
- No additional infrastructure
- Persistent across restarts
- Can query/audit active sessions

**Cons:**
- Database query on every request (performance impact)
- Requires cleanup job for expired tokens
- Slower than Redis

**Not recommended** due to performance concerns.

### Option 3: Short-Lived Tokens Only

**Pros:**
- No infrastructure changes
- Simple to implement

**Cons:**
- Poor UX (frequent re-authentication)
- Doesn't solve the core security issue
- Still vulnerable during token lifetime

**Not recommended** as a standalone solution.

---

## Implementation Checklist

### Phase 1: Core Revocation (High Priority)

- [ ] Create `TokenRevocationPort` interface
- [ ] Implement `RedisTokenRevocationAdapter`
- [ ] Add revocation check to `JwtAuthenticationFilter`
- [ ] Add `POST /api/v1/auth/logout` endpoint
- [ ] Update `TokenClaims` to include `issuedAt` timestamp
- [ ] Add `getTokenExpiration()` to `JwtPort`

### Phase 2: Integration (High Priority)

- [ ] Integrate revocation in `AccountDeletionService.deleteAccount()`
- [ ] Add configuration properties:
  - `app.jwt.revocation.enabled` (default: true)
  - `app.jwt.revocation.use-redis` (default: true)
- [ ] Add fallback `NoOpTokenRevocationAdapter` when disabled
- [ ] Update mobile app to call logout endpoint (optional but recommended)
- [ ] Update admin/web to call logout endpoint

### Phase 3: Advanced Features (Medium Priority)

- [ ] Add "Revoke all sessions" admin endpoint
- [ ] Add "Active sessions" list endpoint for users
- [ ] Add "Revoke on password change" logic
- [ ] Add "Revoke on role change" logic
- [ ] Add metrics: `auth.token.revocation.count`, `auth.token.revocation.check.duration`

### Phase 4: Testing & Documentation (High Priority)

- [ ] Unit tests for `RedisTokenRevocationAdapter`
- [ ] Integration tests for logout flow
- [ ] Integration tests for account deletion with revocation
- [ ] Update `COMPLIANCE.md` with implementation details
- [ ] Update `ARCHITECTURE_AND_FLOWS.md` with logout flow
- [ ] Add ADR documenting revocation strategy choice
- [ ] Update API documentation (OpenAPI/Swagger)

---

## Configuration

### Recommended Settings

```yaml
# application.yml
app:
  jwt:
    secret: ${JWT_SECRET}
    access-expiration-ms: 900000      # 15 minutes
    refresh-expiration-ms: 2592000000 # 30 days
    revocation:
      enabled: true
      use-redis: true

# application-dev.yml (optional: disable for local dev)
app:
  jwt:
    revocation:
      enabled: false
```

### Environment Variables

```bash
JWT_REVOCATION_ENABLED=true
JWT_REVOCATION_USE_REDIS=true
```

---

## Performance Considerations

### Redis Revocation Check

- **Latency:** ~1-2ms per check (local Redis)
- **Throughput:** Redis can handle 100k+ ops/sec
- **Memory:** ~100 bytes per revoked token
- **Cleanup:** Automatic via TTL (no manual cleanup needed)

### Optimization Strategies

1. **Hash tokens before storing** - Reduces key size and adds security
2. **Use Redis pipelining** - Batch multiple checks if needed
3. **Cache negative results** - Short-lived local cache for "not revoked" (optional)
4. **Monitor Redis health** - Fallback to "allow" if Redis is down (configurable)

---

## Security Best Practices

1. **Always hash tokens** before storing in Redis (prevents token leakage from Redis dumps)
2. **Use short access token expiration** (15 minutes recommended)
3. **Longer refresh token expiration** (30 days) with revocation support
4. **Revoke on sensitive actions:**
   - Account deletion
   - Password change
   - Role/permission change
   - Suspicious activity detection
5. **Log all revocation events** for audit trail
6. **Monitor revocation metrics** for anomaly detection

---

## Related Files

- `/backend/src/main/kotlin/com/tamixa/api/config/JwtAuthenticationFilter.kt`
- `/backend/src/main/kotlin/com/tamixa/infrastructure/jwt/JwtService.kt`
- `/backend/src/main/kotlin/com/tamixa/api/auth/AuthController.kt`
- `/backend/src/main/kotlin/com/tamixa/application/account/AccountDeletionService.kt`
- `/docs/COMPLIANCE.md` (Section 13.2)
- `/docs/ARCHITECTURE_AND_FLOWS.md` (Section 6.2)

---

## Conclusion

**JWT revocation is NOT currently implemented** in the Tamixa backend. This represents a **high-severity security gap** that should be addressed before production deployment.

**Recommended Action:** Implement **Option 1 (Redis Token Blacklist)** as it provides the best balance of security, performance, and maintainability with existing infrastructure.

**Estimated Effort:** 2-3 days for core implementation + testing + documentation.
