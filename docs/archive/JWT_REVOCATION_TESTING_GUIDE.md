# JWT Revocation Testing Guide

## Quick Start Testing

### Prerequisites
- Backend running locally (`./gradlew :backend:bootRun`)
- Redis running (`docker run -d -p 6379:6379 redis` or use existing Redis)
- Valid test account or use dev seed admin

### Test 1: Basic Logout Flow

```bash
# 1. Login to get tokens
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@techadivas.com",
    "password": "Admin123!"
  }' | jq

# Save the accessToken from response
export TOKEN="<paste-access-token-here>"

# 2. Verify token works
curl http://localhost:8080/api/v1/auth/me \
  -H "Authorization: Bearer $TOKEN" | jq

# Expected: 200 OK with user details

# 3. Logout
curl -X POST http://localhost:8080/api/v1/auth/logout \
  -H "Authorization: Bearer $TOKEN" | jq

# Expected: {"message":"Logged out successfully"}

# 4. Try to use the same token again
curl http://localhost:8080/api/v1/auth/me \
  -H "Authorization: Bearer $TOKEN" | jq

# Expected: 401 Unauthorized
```

### Test 2: Account Deletion Revokes All Sessions

```bash
# 1. Create a test parent account
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test-delete@example.com",
    "password": "Test123!",
    "acceptedTerms": true,
    "acceptedPrivacy": true,
    "acceptedParentalAttestation": true
  }' | jq

# Save both tokens
export TOKEN1="<paste-access-token-here>"
export REFRESH_TOKEN="<paste-refresh-token-here>"

# 2. Get a second access token via refresh
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\": \"$REFRESH_TOKEN\"}" | jq

export TOKEN2="<paste-new-access-token-here>"

# 3. Verify both tokens work
curl http://localhost:8080/api/v1/auth/me \
  -H "Authorization: Bearer $TOKEN1" | jq

curl http://localhost:8080/api/v1/auth/me \
  -H "Authorization: Bearer $TOKEN2" | jq

# Both should return 200 OK

# 4. Delete the account (using TOKEN1)
curl -X DELETE http://localhost:8080/api/v1/auth/account \
  -H "Authorization: Bearer $TOKEN1" | jq

# Expected: {"message":"Account deleted successfully"}

# 5. Try to use TOKEN1 (should fail - account deleted)
curl http://localhost:8080/api/v1/auth/me \
  -H "Authorization: Bearer $TOKEN1" | jq

# Expected: 401 Unauthorized

# 6. Try to use TOKEN2 (should also fail - all sessions revoked)
curl http://localhost:8080/api/v1/auth/me \
  -H "Authorization: Bearer $TOKEN2" | jq

# Expected: 401 Unauthorized

# 7. Try to refresh (should fail)
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\": \"$REFRESH_TOKEN\"}" | jq

# Expected: 401 Unauthorized or error
```

### Test 3: Verify Redis Storage

```bash
# Connect to Redis
redis-cli

# List all revoked tokens
KEYS auth:revoked:token:*

# List all revoked users
KEYS auth:revoked:user:*

# Check TTL on a revoked token (should be ~900 seconds for access token)
TTL auth:revoked:token:<hash>

# Get user revocation timestamp
GET auth:revoked:user:<hash>

# Should return Unix timestamp (epoch seconds)
```

### Test 4: Revocation Disabled (Dev Mode)

```bash
# 1. Stop backend

# 2. Set environment variable
export JWT_REVOCATION_ENABLED=false

# 3. Start backend
./gradlew :backend:bootRun

# 4. Login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@techadivas.com",
    "password": "Admin123!"
  }' | jq

export TOKEN="<paste-access-token-here>"

# 5. Logout (should succeed but not actually revoke)
curl -X POST http://localhost:8080/api/v1/auth/logout \
  -H "Authorization: Bearer $TOKEN" | jq

# Expected: {"message":"Logged out successfully"}

# 6. Try to use token again (should STILL WORK because revocation is disabled)
curl http://localhost:8080/api/v1/auth/me \
  -H "Authorization: Bearer $TOKEN" | jq

# Expected: 200 OK (token still valid)

# 7. Check logs - should see:
# "JWT token revocation is DISABLED. Tokens cannot be revoked before expiration."
```

### Test 5: Redis Failure Handling (Fail-Open)

```bash
# 1. Start backend with revocation enabled
# 2. Login and get token
# 3. Stop Redis
docker stop <redis-container-id>

# 4. Try to use token (should STILL WORK - fail-open)
curl http://localhost:8080/api/v1/auth/me \
  -H "Authorization: Bearer $TOKEN" | jq

# Expected: 200 OK (authentication continues despite Redis failure)

# 5. Check logs - should see:
# "Redis token revocation check failed, allowing request: <error>"

# 6. Restart Redis
docker start <redis-container-id>
```

---

## Integration Test (Kotlin)

Create this file: `/backend/src/test/kotlin/com/tamixa/api/auth/JwtRevocationIntegrationTest.kt`

```kotlin
package com.tamixa.api.auth

import com.tamixa.application.port.JwtPort
import com.tamixa.application.port.TokenRevocationPort
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.Instant

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JwtRevocationIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var jwtPort: JwtPort

    @Autowired
    private lateinit var tokenRevocationPort: TokenRevocationPort

    @Test
    fun `logout should revoke access token`() {
        // Generate a token
        val token = jwtPort.generateAccessToken("test@example.com", "PARENT")
        
        // Token should work before logout
        mockMvc.perform(
            get("/api/v1/auth/me")
                .header("Authorization", "Bearer $token")
        ).andExpect(status().isOk)
        
        // Logout
        mockMvc.perform(
            post("/api/v1/auth/logout")
                .header("Authorization", "Bearer $token")
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("Logged out successfully"))
        
        // Token should be revoked
        assert(tokenRevocationPort.isRevoked(token))
        
        // Token should not work after logout
        mockMvc.perform(
            get("/api/v1/auth/me")
                .header("Authorization", "Bearer $token")
        ).andExpect(status().isUnauthorized)
    }

    @Test
    fun `user-level revocation should invalidate all tokens`() {
        val email = "test@example.com"
        
        // Generate two tokens at different times
        val token1 = jwtPort.generateAccessToken(email, "PARENT")
        Thread.sleep(1000)
        val token2 = jwtPort.generateAccessToken(email, "PARENT")
        
        // Both tokens should work
        assert(!tokenRevocationPort.isRevoked(token1))
        assert(!tokenRevocationPort.isRevoked(token2))
        
        // Revoke all user sessions
        tokenRevocationPort.revokeAllForUser(email)
        
        // Both tokens should be revoked
        val claims1 = jwtPort.validateAccessToken(token1)!!
        val claims2 = jwtPort.validateAccessToken(token2)!!
        
        assert(tokenRevocationPort.isUserRevoked(email, claims1.issuedAt))
        assert(tokenRevocationPort.isUserRevoked(email, claims2.issuedAt))
    }

    @Test
    fun `logout with invalid token should still succeed`() {
        // Logout with invalid token (idempotent)
        mockMvc.perform(
            post("/api/v1/auth/logout")
                .header("Authorization", "Bearer invalid-token")
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("Logged out successfully"))
    }
}
```

---

## Expected Log Messages

### Successful Logout
```
INFO  c.t.a.a.AuthController - User logged out email=***@example.com
DEBUG c.t.a.c.JwtAuthenticationFilter - Token is revoked
```

### Account Deletion
```
INFO  c.t.a.a.AccountDeletionService - Revoked all sessions for account email=***@example.com
INFO  c.t.a.a.AccountDeletionService - Account deleted successfully email=***@example.com
```

### Revocation Disabled
```
WARN  c.t.i.r.NoOpTokenRevocationAdapter - JWT token revocation is DISABLED. Tokens cannot be revoked before expiration.
DEBUG c.t.i.r.NoOpTokenRevocationAdapter - Token revocation skipped (disabled)
```

### Redis Failure (Fail-Open)
```
WARN  c.t.i.r.RedisTokenRevocationAdapter - Redis token revocation check failed, allowing request: Connection refused
```

---

## Troubleshooting

### Issue: Logout succeeds but token still works

**Possible Causes:**
1. Revocation is disabled (`JWT_REVOCATION_ENABLED=false`)
2. Redis is not running
3. Using a different Redis instance than expected

**Solution:**
```bash
# Check configuration
curl http://localhost:8080/actuator/env | jq '.propertySources[] | select(.name | contains("application")) | .properties."app.jwt.revocation.enabled"'

# Check Redis connection
redis-cli ping
# Should return: PONG

# Check backend logs for:
# "JWT token revocation is DISABLED" or
# "Redis token revocation check failed"
```

### Issue: All authentication fails after enabling revocation

**Possible Causes:**
1. Redis is down
2. Redis connection misconfigured
3. Clock skew between servers

**Solution:**
```bash
# Check Redis health
redis-cli ping

# Check Redis connection in backend logs
# Should NOT see: "Redis token revocation check failed"

# Temporarily disable revocation to restore service
export JWT_REVOCATION_ENABLED=false
# Restart backend
```

### Issue: Tokens not being cleaned up from Redis

**Possible Causes:**
1. TTL not being set correctly
2. Redis persistence disabled

**Solution:**
```bash
# Check TTL on revoked tokens
redis-cli
TTL auth:revoked:token:<hash>

# Should return positive number (seconds until expiration)
# If returns -1, TTL was not set

# Check Redis config
CONFIG GET maxmemory-policy
# Should be: volatile-lru or allkeys-lru for automatic eviction
```

---

## Performance Testing

### Load Test Logout Endpoint

```bash
# Install Apache Bench
brew install httpd  # macOS

# Run load test (1000 requests, 10 concurrent)
ab -n 1000 -c 10 \
  -H "Authorization: Bearer $TOKEN" \
  -m POST \
  http://localhost:8080/api/v1/auth/logout
```

### Monitor Redis Performance

```bash
# Redis CLI
redis-cli

# Monitor all commands
MONITOR

# Check memory usage
INFO memory

# Check command stats
INFO commandstats
```

---

## Checklist

- [ ] Backend compiles successfully
- [ ] Redis is running and accessible
- [ ] Logout endpoint returns 200 OK
- [ ] Token is revoked after logout (401 on subsequent requests)
- [ ] Account deletion revokes all sessions
- [ ] Multiple tokens from same user are all revoked
- [ ] Revocation can be disabled via config
- [ ] Redis failure doesn't break authentication (fail-open)
- [ ] Revoked tokens are cleaned up automatically (TTL)
- [ ] Logs show appropriate messages
- [ ] Integration tests pass
- [ ] Documentation updated (COMPLIANCE.md, ARCHITECTURE_AND_FLOWS.md)

---

## Next Steps

1. ✅ Implementation complete
2. ⏳ Run manual tests (this guide)
3. ⏳ Write integration tests
4. ⏳ Deploy to staging
5. ⏳ Monitor Redis performance
6. ⏳ Update mobile/web/admin clients to call logout endpoint
7. ⏳ Add password change revocation (future enhancement)
8. ⏳ Add metrics and alerting (future enhancement)
