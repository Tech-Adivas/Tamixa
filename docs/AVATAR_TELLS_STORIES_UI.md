# How the Uploaded Avatar (Favorite Person Photo) Tells Stories — UI Implementation

**Expectation:** The parent uploads a favorite person’s photo (avatar). That avatar should “tell” the story (talking-head video with lip-sync).

**Important:** **Vertex AI does not provide “photo + audio → talking-head video.”** Google Cloud Vertex AI offers Gemini (text/audio), Veo (text→video), and Cloud TTS — but **no API that turns a single photo + narration audio into a lip-synced talking-head video**. That capability is provided by **HeyGen** (primary in this app), **Replicate (SadTalker)**, **D-ID**, or **Gooey**. So “implement in Vertex UI” for avatar video is not possible; the implementation uses the existing **Admin UI** and **Mobile UI** with HeyGen/Replicate on the backend.

Below: how the **uploaded avatar tells stories** flow is implemented in the **app UI** (Admin and Mobile), and where Vertex AI can still be used (story text / TTS only).

---

## 1. Architecture in one sentence

**Parent uploads avatar image → Backend uses HeyGen (or Replicate/D-ID) to create talking-head video from that photo + story narration audio → App plays the video so “the avatar tells the story.”**

- **Avatar video (lip-sync):** HeyGen or fallbacks (Replicate, D-ID, Gooey) — **not Vertex AI**.
- **Story text / TTS:** Can use Vertex AI (Gemini, Cloud TTS) if you integrate them; avatar video path is unchanged.

---

## 2. Admin UI: Voice & Avatar Studio (test “avatar tells story”)

Use this to verify that the **uploaded avatar tells the story** before testing on mobile.

### 2.1 Where

- **Route:** Dashboard → **Voice & Avatar Studio** (e.g. `/dashboard/voice-test`).
- **File:** `admin/src/app/(dashboard)/dashboard/voice-test/page.tsx`.

### 2.2 Steps in the UI

1. **Select parent**  
   - Choose the parent who will “be” the avatar (the one whose favorite person photo you upload).

2. **Upload avatar (favorite person photo)**  
   - In the **Avatar** card: choose a face photo (JPEG/PNG, max 5MB).  
   - Click **Upload avatar**.  
   - Backend stores it; it is used as the talking-head source for all avatar videos for this parent.

3. **Set story and voice**  
   - **Story:** Pick the story that the avatar will “tell.”  
   - **Language:** e.g. Tamil, English, Hindi.  
   - **Cloned voice:** Select a cloned voice profile (e.g. `cloned:1`). The story will be narrated in that voice and that audio will drive the avatar’s lip-sync.

4. **Step 1 — Hear narration**  
   - Play the story audio (cloned voice). This generates or reuses the narration used for the avatar video.

5. **Step 2 — “Play with avatar”**  
   - Click **Play with avatar**.  
   - Backend: takes the **uploaded avatar image** + **cloned-voice narration URL** → calls HeyGen (or Replicate/D-ID) → gets back a talking-head video → stores in S3 and returns `avatarVideoUrl`.  
   - The page polls until the video is READY, then shows the video: **the uploaded avatar is now “telling” the story**.

6. **Optional: Regenerate**  
   - Use **Regenerate avatar** to create a new video (e.g. after changing avatar image or voice).

### 2.3 Backend configuration (required for avatar video)

Avatar video is **not** Vertex AI; it uses the existing lip-sync provider. In backend `.env`:

- **HeyGen (recommended):**
  ```bash
  AVATAR_VIDEO_ENABLED=true
  AVATAR_VIDEO_PROVIDER=heygen
  HEYGEN_API_KEY=your_key
  ```
- **Or Replicate (fallback):**
  ```bash
  AVATAR_VIDEO_ENABLED=true
  AVATAR_VIDEO_PROVIDER=sadtalker
  REPLICATE_API_TOKEN=r8_...
  ```

See [AVATAR_VIDEO.md](AVATAR_VIDEO.md) for D-ID and Gooey.

---

## 3. Mobile UI: Parent uploads avatar and then hears/sees avatar tell the story

### 3.1 Uploading the avatar (favorite person photo)

- **Entry points:**  
  - **My Voice & Avatar** hub → **Avatar** (or “Upload avatar”).  
  - Or **Settings** / **Profile** → link to avatar upload.
- **Screen:** `AvatarUploadScreen` (`mobile/.../AvatarUploadScreen.kt`).
- **Flow:**
  1. Parent picks or captures a face photo (JPEG/PNG).
  2. App calls `POST /api/v1/parents/me/avatar` (multipart) → backend stores image and returns `avatarUrl`.
  3. This image is the one used for all “avatar tells story” videos for that parent.

### 3.2 Playing a story “told by” the avatar

- **Entry:** Parent opens a **story** and chooses **cloned voice** (and is on a plan that includes avatar).
- **Flow:**
  1. App calls `GET /api/v1/stories/:id/stream-url?language=...&voiceProfile=cloned:X`.
  2. Backend returns `streamUrl` (audio), `avatarUrl` (static image), and — when ready — `avatarVideoUrl` (talking-head video).
  3. If `avatarVideoUrl` is present, the app uses **AvatarVideoSurface** (ExoPlayer/AVPlayer) to play the video: **the uploaded avatar is telling the story**.
  4. If the video is still generating, backend may return status PENDING; app can show static avatar + audio and retry or poll until `avatarVideoUrl` is available.

So in the **mobile UI**, no extra “Vertex UI” is involved: the same story playback screen shows either audio-only or **avatar video** when the backend has generated it from the **uploaded avatar** + cloned-voice narration.

---

## 4. Where Vertex AI fits (optional)

If you want to use **Vertex AI** in the product:

- **Story generation / rewrite / translation:** Use **Vertex AI Gemini** (e.g. Gemini 2.5 Flash) for text. Integrate in your existing story/translation pipeline; the **avatar path does not change**.
- **Default or cloned TTS:** Use **Google Cloud TTS** (Neural2, Chirp, etc.) or Vertex Gemini TTS from your backend. Again, the avatar pipeline still uses **narration audio URL** → HeyGen/Replicate → talking-head video.
- **Vertex AI Studio (Google Cloud console):** Use it to prototype **Gemini** prompts or **TTS**; it does **not** replace or implement “avatar tells story” — that stays in your app UI + HeyGen/Replicate.

**Summary:** “Uploaded avatar tells stories” is implemented in **Admin (Voice & Avatar Studio)** and **Mobile (avatar upload + story playback)**. Vertex AI is not used for the talking-head video; it can be used for story text and TTS only.

---

## 5. Checklist: “Uploaded avatar tells story” in the UI

| Step | Admin UI | Mobile UI |
|------|----------|-----------|
| 1. Upload avatar (favorite person photo) | Voice & Avatar Studio → Avatar card → Upload | My Voice & Avatar / Settings → Avatar upload |
| 2. Pick story + cloned voice | Story, Language, Cloned voice dropdowns | Story → select cloned voice |
| 3. Generate / play avatar video | Step 2: “Play with avatar” → poll → video shown | Open story with cloned voice → backend returns `avatarVideoUrl` → video plays |
| 4. Backend (required) | `AVATAR_VIDEO_PROVIDER=heygen` (or sadtalker) + API key | Same backend |

No Vertex UI is required for the avatar; the existing Admin and Mobile UIs implement the full flow.
