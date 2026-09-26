# Cover Image Loading Debug Guide

## Problem
Cover images not displaying in mobile app (all sections: Library, Spotlight, Continue Listening, etc.)

## Root Cause Analysis

### What We Know
1. **Backend is configured correctly**:
   - S3 storage type: `s3`
   - S3 bucket: `arn:aws:s3:us-east-1:697304658590:accesspoint/araro-sudio-s3` (Access Point ARN)
   - S3 region: `us-east-1`
   - AWS credentials are loaded from `.env`

2. **Code flow is correct**:
   - `LibraryStoryController` → `StoryLibraryService.findByLanguageApprovedOnly()`
   - Service calls `coverImageUrlResolver.resolveCoverPath(it.coverImageUrl)`
   - Resolver tries to generate S3 presigned URLs (60 min expiry)
   - Falls back to proxy paths if presigning fails

3. **Potential issues**:
   - S3 Access Point ARNs may require special handling for presigning
   - S3Presigner might be failing silently
   - Database might contain invalid cover image paths

## Debug Steps

### Step 1: Start Backend with Debug Logging
```bash
./start-backend-debug.sh
```

This will:
- Kill any existing backend process
- Start backend with DEBUG logging for cover image resolution
- Log output to both console and `backend-debug.log`

### Step 2: Test the Debug Endpoint
```bash
# Test with a known cover image path
curl "http://localhost:8080/api/v1/debug/cover-url?path=curated_covers/86.png"
```

Expected response:
```json
{
  "inputPath": "curated_covers/86.png",
  "resolvedUrl": "https://...",  // Should be S3 presigned URL or proxy path
  "directS3Url": "https://...",   // Direct S3 presigned URL
  "s3SignedUrlGeneratorPresent": true,
  "resolvedUrlLength": 500,
  "directS3UrlLength": 500
}
```

### Step 3: Check Backend Logs
Look for these log patterns in `backend-debug.log`:

**Success pattern:**
```
🔍 resolveCoverPath called with path=curated_covers/86.png
🔍 Path is S3 key: curated_covers/86.png, s3SignedUrlGenerator present: true
🔍 signUrl called: key=curated_covers/86.png, expiryMin=60, bucket=arn:aws:s3:us-east-1:697304658590:accesspoint/araro-sudio-s3
✅ S3 presigned URL generated: key=curated_covers/86.png, expiryMin=60, url=https://...
✅ Generated S3 presigned URL for path=curated_covers/86.png, url length=500
```

**Failure pattern:**
```
🔍 resolveCoverPath called with path=curated_covers/86.png
🔍 Path is S3 key: curated_covers/86.png, s3SignedUrlGenerator present: true
🔍 signUrl called: key=curated_covers/86.png, expiryMin=60, bucket=arn:aws:s3:us-east-1:697304658590:accesspoint/araro-sudio-s3
❌ S3 presign failed: key=curated_covers/86.png, bucket=arn:aws:s3:us-east-1:697304658590:accesspoint/araro-sudio-s3, error=...
⚠️ S3 presigning failed for path=curated_covers/86.png, s3SignedUrlGenerator=..., falling back to proxy
```

### Step 4: Test Mobile App
1. Clear app data: `adb shell pm clear com.tamixa.android`
2. Launch app and navigate to Library
3. Check logcat for image loading:
```bash
adb logcat | grep -E "StoryCoverImage|coverImageUrl"
```

### Step 5: Check Database
```bash
# Connect to database
psql -h localhost -U postgres -d araro_kids

# Check what cover_image_url values are stored
SELECT id, title, cover_image_url FROM curated_story LIMIT 10;
```

Expected values:
- `curated_covers/86.png` (S3 key)
- `https://...` (Full S3 URL)
- `/api/v1/covers/curated_covers/86.png` (Proxy path)

## Possible Fixes

### Fix 1: S3 Access Point ARN Issue
If S3Presigner doesn't support Access Point ARNs directly, extract the bucket name:

```kotlin
// In S3SignedUrlGenerator.kt
private val actualBucket: String get() {
    val bucket = appProperties.storage.effectiveS3Bucket
    // Extract bucket name from Access Point ARN
    // arn:aws:s3:us-east-1:697304658590:accesspoint/araro-sudio-s3 → araro-sudio-s3
    return if (bucket.startsWith("arn:aws:s3:")) {
        bucket.substringAfterLast("/")
    } else {
        bucket
    }
}
```

### Fix 2: Use Proxy URLs Only
If S3 presigning continues to fail, revert to proxy-only approach:

```kotlin
// In CoverImageUrlResolver.kt
fun resolveCoverPath(path: String?): String? {
    if (path.isNullOrBlank()) return null
    if (!path.startsWith("covers/") && !path.startsWith("curated_covers/")) return null
    // Always use proxy (works with emulator and real devices)
    return "${ApiVersion.V1}/covers/$path"
}
```

Then update mobile app's `BASE_URL` to use network IP:
```kotlin
// In mobile/androidApp/build.gradle.kts
buildConfigField("String", "BASE_URL", "\"http://192.168.0.9:8080\"")
```

### Fix 3: Database Migration
If database contains invalid paths, run migration:

```sql
-- Update paths to use S3 keys
UPDATE curated_story 
SET cover_image_url = REGEXP_REPLACE(cover_image_url, '^.*/covers/', '')
WHERE cover_image_url LIKE '%/covers/%';
```

## Files Modified

1. `backend/src/main/kotlin/com/tamixa/application/stream/CoverImageUrlResolver.kt`
   - Added comprehensive debug logging
   - Added fallback to proxy URLs if S3 presigning fails

2. `backend/src/main/kotlin/com/tamixa/infrastructure/cdn/S3SignedUrlGenerator.kt`
   - Added debug logging with emojis for easy identification
   - Changed error logging from WARN to ERROR with full stack trace

3. `backend/src/main/kotlin/com/tamixa/api/debug/DebugController.kt`
   - New debug endpoint for testing cover URL resolution
   - Returns detailed information about URL generation

## Next Actions

1. **Run the debug script**: `./start-backend-debug.sh`
2. **Test the debug endpoint**: Check what URLs are being generated
3. **Review logs**: Look for S3 presigning errors
4. **Apply appropriate fix**: Based on the error messages
5. **Test mobile app**: Verify images load correctly

## Contact
If issues persist after following this guide, provide:
- Output from debug endpoint
- Relevant lines from `backend-debug.log`
- Mobile app logcat output
- Database query results
