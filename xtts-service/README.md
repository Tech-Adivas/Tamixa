# XTTS voice cloning service

**Permanent solution (no Python/XTTS required):** Enable **ElevenLabs** in the backend (`app.voice-cloning.enabled=true`, `app.voice-cloning.eleven-labs-api-key` set). The backend will create an ElevenLabs voice from the stored reference on first use and narrate the story in that voice. Requires S3 for reference storage. You do not need to run this XTTS service.

This service is for **self-hosted** voice cloning when you prefer not to use ElevenLabs. The backend calls `POST /synthesize` with:

- **text** – story content to narrate (plain text, SSML stripped)
- **reference_path** – storage path for the voice reference (e.g. `voices/18/file.mp3`)
- **language** – e.g. `ta`, `en`
- **reference_url** – (optional) presigned URL to download the reference audio; when present, the service uses this to fetch the file and synthesize the story in that voice

## Two modes

### 1. Stub (default)

With only `requirements.txt` installed, the service **does not** synthesize. It always returns `tamixa.mp3`, so you hear the same short sample regardless of story or voice.

### 2. Real synthesis (Coqui XTTS)

Coqui TTS supports **Python 3.9, 3.10, or 3.11 only** (not 3.12+). Use a dedicated venv.

**Install Python 3.11 on macOS** (if you don’t have it):

- **Homebrew:** `brew install python@3.11`  
  Then use: `/opt/homebrew/opt/python@3.11/bin/python3.11` (Apple Silicon) or `/usr/local/opt/python@3.11/bin/python3.11` (Intel) to create the venv.
- **pyenv:** `pyenv install 3.11.9` then `pyenv local 3.11.9` in this directory.

Then create the venv and install:

```bash
# Use the path from Homebrew if you used brew (Apple Silicon example):
/opt/homebrew/opt/python@3.11/bin/python3.11 -m venv venv-xtts
# Or if python3.11 is on PATH (e.g. after pyenv):
# python3.11 -m venv venv-xtts

source venv-xtts/bin/activate   # Windows: venv-xtts\Scripts\activate
pip install -r requirements.txt -r requirements-xtts.txt
```

Then:

1. **Backend** must send **reference_url** (e.g. presigned S3 URL) so the service can download the reference audio. When the backend uses S3 for voice storage, it does this automatically.
2. The service downloads the reference from `reference_url`, loads the Coqui XTTS v2 model (once), and synthesizes **text** in that voice.
3. Response is **WAV** (`audio/wav`). The admin and backend treat it as opaque audio bytes.

**Synthesis completed or not:** On success, responses include headers so callers don’t have to guess from bytes alone:
- `X-Synthesis-Status: completed` — real XTTS output; body is WAV.
- `X-Synthesis-Bytes`, `X-Synthesis-Chunks` — size and number of text chunks used.
- Stub (no reference): `X-Synthesis-Status: stub` — body is `tamixa.mp3`, not the story.
- On failure: HTTP **503** (Coqui not loaded) or **500** with JSON `detail`; uvicorn prints `SYNTHESIS_NOT_COMPLETED`.

First request after startup may be slow while the model loads. `GET /health` reports `xtts_loaded: true` when the model is ready.

**Apple Silicon (MPS):** The service prefers **MPS** over CPU when `torch.backends.mps.is_available()` (PyTorch Metal). If MPS load fails, it falls back to CPU automatically. To force device: `XTTS_DEVICE=mps|cuda|cpu` before `uvicorn`. If synthesis errors occur on MPS, use `XTTS_DEVICE=cpu`.

### How to get Coqui loaded

1. **Use Python 3.9, 3.10, or 3.11** (Coqui TTS does not support 3.12+).
   - macOS (Homebrew): `brew install python@3.11` then use `/opt/homebrew/opt/python@3.11/bin/python3.11` (Apple Silicon) to create the venv.
   - Or pyenv: `pyenv install 3.11.9` then `pyenv local 3.11.9` in the `xtts-service` directory.

2. **Create a venv and install deps** (from the `xtts-service` folder):
   ```bash
   python3.11 -m venv venv-xtts
   source venv-xtts/bin/activate   # Windows: venv-xtts\Scripts\activate
   pip install -r requirements.txt -r requirements-xtts.txt
   ```
   If the reference audio is **MP3**, the service converts it to WAV using pydub, which needs **ffmpeg**: `brew install ffmpeg` (macOS) or `apt install ffmpeg` (Linux).

   **If you still get `TorchCodec is required for load_with_torchcodec`** after ffmpeg + restart: newer PyTorch/torchaudio loads speaker audio via **torchcodec**. Install it in the same venv:
   ```bash
   pip install torchcodec
   ```
   (`requirements-xtts.txt` already includes `torchcodec`; reinstall if you added deps before that line existed.)

3. **Start the service:**
   ```bash
   uvicorn main:app --host 0.0.0.0 --port 9000
   ```

4. **Trigger model load** (optional; otherwise the first `/synthesize` with reference does it):
   ```bash
   curl http://localhost:9000/preload
   ```

5. **Check that Coqui is loaded:**
   ```bash
   curl http://localhost:9000/health
   ```
   You should see `"xtts_loaded": true`. If you see `false`, check the server logs for the error (e.g. missing `TTS`, wrong Python version, or `transformers` version).

   **If synthesis fails with `'GPT2InferenceModel' object has no attribute 'generate'`:** your `transformers` is 4.50+ (incompatible with the stock `TTS` package). Reinstall with the pinned deps:
   ```bash
   pip install "transformers>=4.42.0,<4.50.0"
   ```
   Or switch to the maintained fork (supports newer transformers): `pip install coqui-tts` and run the same `TTS(...)` API if your code uses it; for this service, sticking to `transformers<4.50` is simplest.

   **If you see `text length exceeds the character limit of 250`:** Coqui truncates long segments per language. This service **chunks** text into ≤240-character segments (sentence-aware when possible), synthesizes each chunk, and concatenates the WAV so the **full story** is narrated. Restart uvicorn after pulling the latest `main.py`.

## Run locally

```bash
pip install -r requirements.txt
# Optional: pip install -r requirements-xtts.txt
uvicorn main:app --host 0.0.0.0 --port 9000
```

**Or run with Docker (Python 3.11 + Coqui pre-installed):**

```bash
docker build -t xtts-service .
docker run -p 9000:9000 xtts-service
```

Ensure `tamixa.mp3` exists for stub mode. The backend uses `XTTS_BASE_URL` (default `http://localhost:9000`) to reach this service.

**Note:** If you're on Python 3.12 or 3.13, `pip install TTS` will fail (Coqui supports up to 3.11). Use the Docker image or a Python 3.11 venv for real synthesis.

## Backend integration

- When **S3** is enabled, the backend sends **reference_url** (presigned) so the XTTS service can fetch the reference file without S3 credentials.
- Without **reference_url** (and without a local file at `reference_path`), the service falls back to returning `tamixa.mp3` (stub).
