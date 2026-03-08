package com.araro.security

import com.araro.platform.currentTimeMillis
import platform.Foundation.NSUserDefaults

class IosTokenStorage : TokenStorage {
    private val defaults = NSUserDefaults.standardUserDefaults
    private val keyPrefix = "araro_secure_"

    override fun getAccessToken(): String? =
        defaults.stringForKey("${keyPrefix}access_token")

    override fun getRefreshToken(): String? =
        defaults.stringForKey("${keyPrefix}refresh_token")

    override fun getTokenExpiryMs(): Long? {
        val expiry = defaults.doubleForKey("${keyPrefix}expiry_ms")
        return if (expiry <= 0) null else expiry.toLong()
    }

    override fun saveTokens(accessToken: String, refreshToken: String, expiresInSeconds: Long) {
        val expiryMs = currentTimeMillis() + expiresInSeconds * 1000
        defaults.setObject(accessToken, forKey = "${keyPrefix}access_token")
        defaults.setObject(refreshToken, forKey = "${keyPrefix}refresh_token")
        defaults.setDouble(expiryMs.toDouble(), forKey = "${keyPrefix}expiry_ms")
        defaults.synchronize()
    }

    override fun clear() {
        listOf("access_token", "refresh_token", "expiry_ms").forEach { key ->
            defaults.removeObjectForKey("${keyPrefix}$key")
        }
        defaults.synchronize()
    }
}
