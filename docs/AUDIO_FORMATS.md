# Audio format support

The app supports **multiple audio formats** for streaming, download, and upload.

## Supported formats

| Format | Extension | MIME type       | Streaming | Download | Upload (e.g. voice) |
|--------|-----------|-----------------|-----------|----------|----------------------|
| MP3    | .mp3      | audio/mpeg      | ✅        | ✅       | ✅                   |
| M4A/AAC| .m4a      | audio/mp4       | ✅        | ✅       | ✅ (family voice)    |
| AAC    | .aac      | audio/aac       | ✅        | ✅       | —                    |
| OGG    | .ogg      | audio/ogg       | ✅        | ✅       | —                    |
| WAV    | .wav      | audio/wav       | ✅        | ✅       | —                    |
| WebM   | .webm     | audio/webm      | ✅        | ✅       | —                    |

- **Streaming**: Backend proxy (`/audio/stories/...`) sets `Content-Type` from the object key extension. Presigned S3 URLs use the Content-Type stored on the object at upload time.
- **Download**: Mobile maps response or explicit MIME type to file extension (Android/iOS `downloadFile`). Video (e.g. avatar) uses `.mp4`.
- **Upload**: Family voice accepts MP3 and M4A (content type + extension passed to storage). Narration TTS output is currently MP3.

## Backend

- **AudioStreamProxyController**: `contentTypeForKey(key)` maps extension (mp3, m4a, mp4, aac, ogg, wav, webm) to the correct `Content-Type` for full and range responses.
- **Family voice**: `FamilyVoiceStoragePort.upload(..., contentType, fileExt)` supports `audio/mpeg`/mp3 and `audio/mp4`/m4a. Path: `families/{parentId}/stories/{storyId}/{language}.{ext}`.
- **Narration**: Stored as `stories/{storyId}/{language}/{voiceSlug}.mp3` (TTS output is MP3). Proxy serves any key under `stories/` with the appropriate Content-Type.

## Mobile

- **Playback**: ExoPlayer (Android) and AVPlayer (iOS) support the formats above when the URL is streamed with the correct Content-Type.
- **Download**: `PlatformApi.downloadFile(..., mimeType)` maps MIME to extension (video → mp4, audio/mp4 → m4a, audio/mpeg → mp3, aac, ogg, wav, webm).
- **Voice picker**: Launches with `audio/*`; backend accepts multipart with `Content-Type: audio/mpeg` or `audio/mp4` and stores with the matching extension.
