# Tamixa CDN Streaming Architecture (Google Cloud)

## Architecture Diagram (Text Format)

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                    TAMIXA AUDIO STREAMING (GOOGLE CLOUD)                                   │
└─────────────────────────────────────────────────────────────────────────────────────────┘

                                    ┌──────────────┐
                                    │ Mobile App   │
                                    │ (ExoPlayer)  │
                                    └──────┬───────┘
                                           │
                        1. GET /stories/{id}/stream-url?language=ta
                                           │
                                           ▼
┌──────────────────────────────────────────────────────────────────────────────────────────┐
│                          BACKEND (Spring Boot)                                            │
│  ┌─────────────────┐    ┌──────────────────┐    ┌─────────────────────┐                 │
│  │ StreamUrlController│───▶│ AudioStreamService │───▶│ GcsSignedUrlGenerator │                 │
│  │ JWT validated     │    │ Check READY/PENDING│    │ V4 signing, 8min TTL   │                 │
│  └─────────────────┘    └────────┬─────────┘    └─────────────────────┘                 │
│           │                       │                                                        │
│           │                       │ Path: stories/{id}/{lang}/audio.mp3                    │
│           │                       ▼                                                        │
│           │              ┌─────────────────┐    ┌─────────────────┐                       │
│           │              │ CuratedStoryRepo │    │ StoryRepo       │                       │
│           │              │ StoryAudioRepo   │    │                 │                       │
│           │              └─────────────────┘    └─────────────────┘                       │
│           │                                                                               │
│           │  2. Response: { "streamUrl": "https://storage.googleapis.com/...?X-Goog-..." }
│           ▼                                                                               │
│  ┌─────────────────┐    ┌──────────────────┐                                             │
│  │ StreamAccessLogger│   │ StreamMetrics    │                                             │
│  │ (audit trail)    │   │ (Prometheus)     │                                             │
│  └─────────────────┘    └──────────────────┘                                             │
└──────────────────────────────────────────────────────────────────────────────────────────┘
                                           │
                        3. Request audio with signed URL (Range: bytes=0-)
                                           │
                                           ▼
┌──────────────────────────────────────────────────────────────────────────────────────────┐
│                    GOOGLE CLOUD STORAGE                                                   │
│  Path: gs://tamixa-audio/stories/{storyId}/{language}/audio.mp3                             │
│  • Uniform bucket-level access; public access prevention                                   │
│  • Signed URLs (V4); supports byte-range requests                                          │
│  • GCS has global edge caching; optional Cloud CDN for LB in front                         │
└──────────────────────────────────────────────────────────────────────────────────────────┘
```

## Flow Sequence

```
Mobile App                Backend                    GCS
     │                        │                         │
     │  GET /stream-url       │                         │
     │  (Bearer JWT)          │                         │
     │───────────────────────>│                         │
     │                        │  Validate JWT           │
     │                        │  Check story READY      │
     │                        │  Generate signed URL    │
     │                        │  Log access             │
     │  { streamUrl }         │                         │
     │<───────────────────────│                         │
     │                        │                         │
     │  GET signed URL        │                         │
     │  Range: bytes=0-       │                         │
     │─────────────────────────────────────────────────>│
     │  206 Partial Content   │                         │
     │<─────────────────────────────────────────────────│
```

## 1. Audio Storage (GCS)

| Aspect | Detail |
|--------|--------|
| Format | MP3 |
| Path | `stories/{storyId}/{language}/audio.mp3` |
| Bucket | Private (public access prevention) |
| Example | `gs://tamixa-audio/stories/42/ta/audio.mp3` |

## 2. Signed URL Generation

| Endpoint | Method | Auth | Description |
|----------|--------|------|-------------|
| `/api/v1/stories/{id}/stream-url` | GET | JWT (PARENT) | Unified: curated or generated |
| `/api/v1/stories/curated/{id}/stream-url` | GET | JWT | Curated stories only |
| `/api/v1/stories/generated/{id}/stream-url` | GET | JWT | AI-generated only |

**Query params:** `language` (default: `ta`)

**Response:** `{ "streamUrl": "https://storage.googleapis.com/tamixa-audio/stories/42/ta/audio.mp3?X-Goog-Algorithm=...&X-Goog-Signature=..." }`

**Expiry:** 5–10 minutes (config: `cdn.signed-url-expiry-minutes`)

## 3. Range Request Support

- GCS supports `Range` headers natively
- Returns `206 Partial Content` with `Content-Range`
- ExoPlayer/AVPlayer support range requests by default
- Enables resume playback and scrubbing

## 4. Caching

| Option | Description |
|--------|-------------|
| GCS direct | GCS has global edge caching; good for most use cases |
| Cloud CDN | Add HTTP(S) Load Balancer + Cloud CDN in front of a backend bucket for additional edge caching |

## 5. Security

| Layer | Measure |
|-------|---------|
| GCS | Public access prevention; signed URLs only |
| Backend | JWT validation before signed URL; log stream access |
| Tokens | Short-lived (8 min); V4 signature |

## 6. Performance Metrics

| Metric | Type | Description |
|--------|------|-------------|
| `stream_url_generation_latency` | Timer | Backend: time to generate signed URL |
| `stream_url_requests` | Counter | Backend: total stream URL requests |
| `stream_start_latency` | DistributionSummary | Client: request to first byte (ms) |
| `stream_buffering_events` | Counter | Client: buffering occurrences |
| `stream_audio_completions` | Counter | Client: playback completed |

**Client analytics:** POST `/api/v1/stories/stream/analytics`

```json
{
  "storyId": 42,
  "streamStartLatencyMs": 245.5,
  "bufferingEvent": false,
  "completed": true
}
```

---

## Configuration (application.yml / env)

```yaml
app:
  cdn:
    enabled: true
    gcp-bucket: tamixa-audio
    gcp-service-account-json-base64: <optional; uses GOOGLE_APPLICATION_CREDENTIALS if blank>
    signed-url-expiry-minutes: 8
    cache-ttl-seconds: 86400
```

**Environment variables:**

| Variable | Description |
|----------|-------------|
| `CDN_STREAM_ENABLED` | Enable CDN mode |
| `CDN_GCP_BUCKET` | GCS bucket name |
| `CDN_GCP_SERVICE_ACCOUNT_JSON_BASE64` | Base64-encoded service account JSON (optional; use ADC otherwise) |
| `CDN_SIGNED_URL_EXPIRY_MINUTES` | URL validity (5–10) |
| `GOOGLE_APPLICATION_CREDENTIALS` | Path to service account JSON file (when not using base64) |

---

## GCP Setup (Terraform)

### 1. Create resources

```bash
cd infra/terraform
cp terraform.tfvars.example terraform.tfvars
# Edit terraform.tfvars with your project_id
terraform init
terraform plan
terraform apply
```

### 2. Service account for the backend

Create a service account (or use the one from Terraform `tamixa-audio-signer`) with:
- `roles/storage.objectViewer` on the bucket
- Used to sign URLs (via ADC or `CDN_GCP_SERVICE_ACCOUNT_JSON_BASE64`)

### 3. TTS pipeline

TTS pipeline must upload to GCS:

```
gs://tamixa-audio/stories/{storyId}/{language}/audio.mp3
```

Convert text → MP3, then use `Storage.create(BlobInfo, bytes)` or `blob.create()`.

---

## Mobile Client Integration

```kotlin
// When playing audio
suspend fun getStreamUrl(storyId: Long, language: String): String? {
    return api.getStreamUrl(storyId, language)
}

// ExoPlayer handles range requests by default
val mediaItem = MediaItem.fromUri(streamUrl)
player.setMediaItem(mediaItem)
player.prepare()
player.play()
```

---

## Optional: Cloud CDN in front of GCS

For lower latency with edge caching:

1. Create an HTTP(S) Load Balancer
2. Add a **Backend bucket** pointing to your GCS bucket
3. Enable **Cloud CDN** on the backend
4. Use the LB URL instead of `storage.googleapis.com` – you would then need to implement Cloud CDN signed URLs (different signing) or keep using GCS signed URLs (which point to storage.googleapis.com). For Cloud CDN with private content, consider signed URLs/cookies on the LB.

For simplicity, direct GCS signed URLs are recommended unless you need LB-level caching.
