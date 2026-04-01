# Certificate Pinning Setup Guide

## Overview

Certificate pinning is implemented for `api.tamixa.com` on both Android and iOS platforms. This guide explains how to update the placeholder certificates with production values.

## Current Status

⚠️ **Placeholder certificates are currently in use. Update before production deployment.**

## Getting Production Certificates

### Step 1: Extract Certificate from Production Server

```bash
# Get the certificate chain from your production server
openssl s_client -connect api.tamixa.com:443 -showcerts < /dev/null 2>/dev/null | \
  openssl x509 -outform PEM > api_tamixa_com.pem

# Extract the public key and generate SHA-256 hash (base64)
openssl x509 -in api_tamixa_com.pem -pubkey -noout | \
  openssl pkey -pubin -outform der | \
  openssl dgst -sha256 -binary | \
  openssl enc -base64
```

### Step 2: Get Backup Certificate

**Option A: Backup from same CA**
```bash
# If you have a backup certificate from the same CA
openssl x509 -in backup_cert.pem -pubkey -noout | \
  openssl pkey -pubin -outform der | \
  openssl dgst -sha256 -binary | \
  openssl enc -base64
```

**Option B: Pin CA certificate**
```bash
# Pin the CA's public key as backup
openssl s_client -connect api.tamixa.com:443 -showcerts < /dev/null 2>/dev/null | \
  sed -n '/-----BEGIN CERTIFICATE-----/,/-----END CERTIFICATE-----/p' | \
  tail -n +2 | \
  openssl x509 -pubkey -noout | \
  openssl pkey -pubin -outform der | \
  openssl dgst -sha256 -binary | \
  openssl enc -base64
```

## Updating Android Configuration

**File:** `mobile/androidApp/src/main/res/xml/network_security_config.xml`

```xml
<pin-set expiration="2026-12-31">
    <!-- Replace with actual certificate fingerprint -->
    <pin digest="SHA-256">YOUR_PRIMARY_CERT_HASH_HERE=</pin>
    <!-- Replace with backup certificate fingerprint -->
    <pin digest="SHA-256">YOUR_BACKUP_CERT_HASH_HERE=</pin>
</pin-set>
```

**Update expiration date** to match your certificate's expiration (or earlier for rotation).

## Updating iOS Configuration

**File:** `mobile/composeApp/src/iosMain/kotlin/com/tamixa/network/CertificatePinning.kt`

```kotlin
private val PINNED_CERTIFICATES = setOf(
    "YOUR_PRIMARY_CERT_HASH_HERE=", // Primary cert
    "YOUR_BACKUP_CERT_HASH_HERE="   // Backup cert
)
```

## Testing Certificate Pinning

### Local Testing

1. **Test with correct certificate:**
   ```bash
   # Should succeed
   curl https://api.tamixa.com/api/v1/health
   ```

2. **Test with wrong certificate (simulate MITM):**
   - Use a proxy with self-signed certificate
   - App should reject the connection

### Automated Testing

```kotlin
// Add to mobile test suite
@Test
fun testCertificatePinning() {
    // Attempt connection to api.tamixa.com
    // Should succeed with valid cert
    
    // Attempt connection with invalid cert
    // Should fail with SSLPeerUnverifiedException
}
```

## Certificate Rotation Strategy

### Before Expiration

1. **90 days before expiration:**
   - Obtain new certificate
   - Calculate new SHA-256 hash
   - Add new hash to pinning configuration (keep old hash)
   - Deploy app update

2. **After app adoption (>95% users updated):**
   - Remove old certificate hash
   - Deploy backend with new certificate only

### Emergency Rotation

If certificate is compromised:

1. **Immediate:**
   - Revoke compromised certificate
   - Deploy new certificate to backend
   - Push emergency app update with new hash

2. **Fallback:**
   - Temporarily disable pinning via remote config (if implemented)
   - Force app update

## Monitoring

### Backend Metrics

Monitor for pinning failures:
```kotlin
// Add to ApplicationMetrics
counter("certificate_pinning_failures")
```

### Client-Side Logging

```kotlin
// In CertificatePinning.kt
if (!result) {
    logger.error("Certificate pinning failed for $host")
    // Report to analytics
}
```

## Security Considerations

1. **Never commit private keys** to version control
2. **Use environment-specific certificates** (staging vs. production)
3. **Pin at least 2 certificates** (primary + backup)
4. **Set reasonable expiration dates** (not too far in future)
5. **Monitor certificate expiration** (automated alerts)

## Troubleshooting

### Common Issues

**Issue:** App can't connect to API after pinning update
- **Cause:** Incorrect certificate hash
- **Fix:** Verify hash matches production certificate

**Issue:** Pinning works on Android but not iOS
- **Cause:** Different hash calculation or encoding
- **Fix:** Ensure both use SHA-256 of public key (not certificate)

**Issue:** Localhost/emulator connections fail
- **Cause:** Pinning applied to development URLs
- **Fix:** Verify bypass logic for localhost/10.0.2.2/127.0.0.1

## Checklist Before Production

- [ ] Extract production certificate hashes
- [ ] Update Android `network_security_config.xml`
- [ ] Update iOS `CertificatePinning.kt`
- [ ] Set appropriate expiration dates
- [ ] Test on physical devices (not emulators)
- [ ] Verify localhost bypass still works
- [ ] Document certificate rotation schedule
- [ ] Set up expiration monitoring
- [ ] Test MITM attack scenario
- [ ] Verify backup certificate works

## References

- [OWASP Certificate Pinning](https://owasp.org/www-community/controls/Certificate_and_Public_Key_Pinning)
- [Android Network Security Config](https://developer.android.com/training/articles/security-config)
- [iOS App Transport Security](https://developer.apple.com/documentation/security/preventing_insecure_network_connections)

---

**Last Updated:** March 2025  
**Next Review:** Before production deployment
