"""
XTTS-style voice cloning service.

- If Coqui TTS is installed and reference audio is provided (reference_audio_base64, reference_url,
  or local reference_path), synthesizes the story text in the cloned voice and returns WAV audio.
- If reference is provided but TTS is not loaded (e.g. Python 3.13): returns 503 with instructions.
- If no reference: returns tamixa.mp3 (stub).

Backend can send reference_audio_base64 (bytes as base64), reference_url (presigned S3), or use local reference_path.
"""

from __future__ import annotations

import base64
import contextlib
import html
import io
import os
import re
import sys
import tempfile
import urllib.request
from contextlib import contextmanager

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from starlette.responses import Response

app = FastAPI(title="XTTS Voice Cloning")

# Lazy-loaded Coqui TTS model (set on first successful use)
_tts_model = None
_tts_load_error = None  # Last exception message when load failed (so /health can show why)
_XTTS_SAMPLE_RATE = 24000
# Coqui XTTS warns above ~250 chars per call for some languages (e.g. hi); chunk to avoid truncated audio.
_XTTS_CHUNK_MAX_CHARS = 240
# Short silence between chunks (seconds) so concatenation sounds natural
_XTTS_CHUNK_GAP_SEC = 0.12

TTS_REQUIRED_MSG = (
    "Real story-in-cloned-voice requires Coqui TTS (Python 3.9–3.11). "
    "Use: python3.11 -m venv venv-xtts && source venv-xtts/bin/activate && pip install -r requirements.txt -r requirements-xtts.txt"
)


class SynthesizeRequest(BaseModel):
    text: str
    reference_path: str
    language: str = "en"
    reference_url: str | None = None  # Presigned URL to fetch reference audio
    reference_audio_base64: str | None = None  # Reference audio bytes (backend sends when S3 available)
    # When backend normalizes (e.g. ta → hi for XTTS), so we can log user's selection
    requested_language: str | None = None


def _get_tts():
    """Lazy-load Coqui XTTS v2. Returns None if TTS is not installed or model fails."""
    global _tts_model, _tts_load_error
    if _tts_model is False:
        return None  # Already tried and failed
    if _tts_model is not None:
        return _tts_model
    try:
        import torch
        # PyTorch 2.6+ defaults to weights_only=True; Coqui checkpoints use custom classes.
        if getattr(torch.load, "_tamixa_weights_patch", None) is None:
            _orig = torch.load
            def _patched(*args, **kwargs):
                if "weights_only" not in kwargs:
                    kwargs["weights_only"] = False
                return _orig(*args, **kwargs)
            _patched._tamixa_weights_patch = True
            torch.load = _patched
        from TTS.api import TTS

        def _pick_device() -> str:
            # Optional override: XTTS_DEVICE=cuda|mps|cpu
            forced = (os.environ.get("XTTS_DEVICE") or "").strip().lower()
            if forced in ("cuda", "mps", "cpu"):
                if forced == "cuda" and not torch.cuda.is_available():
                    return "cpu"
                if forced == "mps":
                    mps_ok = getattr(torch.backends, "mps", None) and torch.backends.mps.is_available()
                    return "mps" if mps_ok else "cpu"
                return forced
            if torch.cuda.is_available():
                return "cuda"
            if getattr(torch.backends, "mps", None) and torch.backends.mps.is_available():
                return "mps"
            return "cpu"

        model_name = "tts_models/multilingual/multi-dataset/xtts_v2"
        device = _pick_device()
        try:
            _tts_model = TTS(model_name).to(device)
        except Exception as e:
            if device == "mps":
                print("[XTTS] MPS load/move failed, falling back to CPU: %s" % e)
                device = "cpu"
                _tts_model = TTS(model_name).to("cpu")
            else:
                raise
        _tts_load_error = None
        print("[XTTS] Model loaded successfully (device=%s)" % device)
        return _tts_model
    except Exception as e:
        err_msg = "%s: %s" % (type(e).__name__, e)
        _tts_load_error = err_msg
        print("[XTTS] Coqui TTS not available, using stub: %s" % err_msg)
        _tts_model = False
        return None


def _ensure_wav(path: str) -> str:
    """Convert MP3 (or other) to WAV so Coqui TTS does not need torchcodec. Returns path to WAV."""
    if path.lower().endswith(".wav"):
        return path
    try:
        from pydub import AudioSegment
        seg = AudioSegment.from_file(path)
        base, _ = os.path.splitext(path)
        wav_path = base + ".wav"
        seg.export(wav_path, format="wav")
        return wav_path
    except Exception as e:
        raise HTTPException(
            status_code=400,
            detail="Reference audio is not WAV and conversion failed (install pydub and ffmpeg for MP3): %s" % e,
        )


@contextmanager
def _reference_audio_path(
    reference_audio_base64: str | None,
    reference_url: str | None,
    reference_path: str,
):
    """
    Yield a path to the reference audio file (always WAV so Coqui TTS does not need torchcodec).
    Prefer reference_audio_base64, then reference_url, then local reference_path.
    """
    if reference_audio_base64:
        try:
            data = base64.b64decode(reference_audio_base64)
        except Exception as e:
            raise HTTPException(status_code=400, detail="Invalid reference_audio_base64: %s" % e)
        suffix = ".wav" if data[:4] == b"RIFF" else ".mp3"
        with tempfile.NamedTemporaryFile(suffix=suffix, delete=False) as f:
            tmp = f.name
            f.write(data)
        wav_path = _ensure_wav(tmp)
        try:
            yield wav_path
        finally:
            try:
                os.unlink(tmp)
            except OSError:
                pass
            if wav_path != tmp:
                try:
                    os.unlink(wav_path)
                except OSError:
                    pass
        return
    if reference_url:
        try:
            req = urllib.request.Request(reference_url, headers={"User-Agent": "TamixaXTTS/1.0"})
            with urllib.request.urlopen(req, timeout=30) as resp:
                data = resp.read()
        except Exception as e:
            raise HTTPException(status_code=400, detail="Failed to fetch reference_url: %s" % e)
        suffix = ".wav" if data[:4] == b"RIFF" else ".mp3"
        with tempfile.NamedTemporaryFile(suffix=suffix, delete=False) as f:
            tmp = f.name
            f.write(data)
        wav_path = tmp
        try:
            wav_path = _ensure_wav(tmp)
            yield wav_path
        finally:
            try:
                os.unlink(tmp)
            except OSError:
                pass
            if wav_path != tmp:
                try:
                    os.unlink(wav_path)
                except OSError:
                    pass
        return
    if reference_path and os.path.isfile(reference_path):
        # Coqui expects WAV; convert if local file is MP3/other to avoid "TorchCodec is required"
        yield _ensure_wav(reference_path)
        return
    raise HTTPException(
        status_code=400,
        detail="Need reference_audio_base64, reference_url, or a local reference_path file."
    )


def _normalize_tts_text(text: str) -> str:
    """Unescape HTML entities and collapse whitespace so TTS reads natural text."""
    if not text:
        return text
    t = text.strip()
    # Backend may send SSML-stripped text still containing &apos; &quot; etc.
    t = html.unescape(t)
    t = re.sub(r"\s+", " ", t)
    return t


def _chunk_text_for_xtts(text: str, max_chars: int = _XTTS_CHUNK_MAX_CHARS) -> list[str]:
    """
    Split text into segments <= max_chars to avoid XTTS per-pass truncation (e.g. 250 for 'hi').
    Prefers breaking at sentence boundaries; falls back to hard splits.
    """
    text = _normalize_tts_text(text)
    if not text:
        return []
    if len(text) <= max_chars:
        return [text]

    # Split on sentence enders (Latin + Devanagari danda, etc.)
    parts = re.split(r"(?<=[.!?।\n])\s+", text)
    chunks: list[str] = []
    current = ""
    for p in parts:
        p = p.strip()
        if not p:
            continue
        if len(p) > max_chars:
            # Flush current then hard-split long piece
            if current:
                chunks.append(current.strip())
                current = ""
            for i in range(0, len(p), max_chars):
                piece = p[i : i + max_chars].strip()
                if piece:
                    chunks.append(piece)
            continue
        if len(current) + len(p) + 1 <= max_chars:
            current = (current + " " + p).strip() if current else p
        else:
            if current:
                chunks.append(current.strip())
            current = p
    if current:
        chunks.append(current.strip())
    return [c for c in chunks if c]


@contextlib.contextmanager
def _suppress_coqui_stdout():
    """Coqui TTS prints 'Text splitted to sentences' every tts.tts() call; suppress to avoid duplicate/noisy logs."""
    with open(os.devnull, "w", encoding="utf-8") as devnull:
        old_out, old_err = sys.stdout, sys.stderr
        try:
            sys.stdout = sys.stderr = devnull
            yield
        finally:
            sys.stdout, sys.stderr = old_out, old_err


def _synthesize_with_xtts(
    text: str, speaker_wav_path: str, language: str, requested_language: str | None = None
) -> tuple[bytes, int]:
    """Run Coqui XTTS and return (WAV bytes, chunk_count). Chunks long text to avoid 250-char truncation."""
    import numpy as np
    try:
        from scipy.io import wavfile
    except ImportError:
        raise HTTPException(
            status_code=503,
            detail="scipy required for XTTS output. pip install scipy",
        )

    tts = _get_tts()
    if tts is None:
        raise HTTPException(
            status_code=503,
            detail="Coqui TTS not available. Install: pip install TTS scipy. Using stub returns tamixa.mp3."
        )

    chunks = _chunk_text_for_xtts(text)
    if not chunks:
        raise HTTPException(status_code=400, detail="Empty text after normalization")

    gap_samples = int(_XTTS_SAMPLE_RATE * _XTTS_CHUNK_GAP_SEC)
    gap = np.zeros(gap_samples, dtype=np.float32)
    segments: list = []

    for i, chunk in enumerate(chunks):
        # Coqui returns list of floats or numpy array, 24kHz
        # Suppress Coqui stdout (sentence-split spam) so one POST doesn't flood the terminal
        with _suppress_coqui_stdout():
            wav = tts.tts(text=chunk, speaker_wav=speaker_wav_path, language=language)
        if isinstance(wav, list):
            wav = np.array(wav, dtype=np.float32)
        else:
            wav = np.asarray(wav, dtype=np.float32)
        wav = np.clip(wav, -1.0, 1.0)
        segments.append(wav)
        if i < len(chunks) - 1:
            segments.append(gap)

    combined = np.concatenate(segments) if len(segments) > 1 else segments[0]
    # Peak normalize so quiet Coqui output doesn't sound like "air only" when played back
    peak = float(np.max(np.abs(combined))) if combined.size else 0.0
    if peak > 1e-6:
        combined = np.clip(combined / peak * 0.95, -1.0, 1.0)
    wav_int16 = (combined * 32767).astype(np.int16)

    buf = io.BytesIO()
    wavfile.write(buf, _XTTS_SAMPLE_RATE, wav_int16)
    buf.seek(0)
    out = buf.read()
    n = len(chunks)
    if requested_language and requested_language != language:
        print(
            "[XTTS] SYNTHESIS_COMPLETED chunks=%d wav_bytes=%d requested=%s → language=%s (XTTS)"
            % (n, len(out), requested_language, language)
        )
    else:
        print("[XTTS] SYNTHESIS_COMPLETED chunks=%d wav_bytes=%d language=%s" % (n, len(out), language))
    return out, n


def _stub_response() -> Response:
    """Return tamixa.mp3 (stub behavior)."""
    try:
        with open("tamixa.mp3", "rb") as f:
            data = f.read()
    except FileNotFoundError:
        raise HTTPException(status_code=500, detail="tamixa.mp3 not found on server")
    return Response(
        content=data,
        media_type="audio/mpeg",
        headers={
            "Content-Disposition": "inline; filename=\"output.mp3\"",
            "X-Synthesis-Status": "stub",
            "X-Synthesis-Message": "no_reference_sample_mp3",
        },
    )


def _has_reference(req: SynthesizeRequest) -> bool:
    return bool(
        req.reference_audio_base64
        or req.reference_url
        or (req.reference_path and os.path.isfile(req.reference_path))
    )


@app.post("/synthesize")
async def synthesize(req: SynthesizeRequest):
    """
    Synthesize story text in the cloned voice.

    - If Coqui TTS is installed and reference is provided (reference_audio_base64, reference_url, or local path),
      returns WAV audio of the story in that voice.
    - If reference is provided but TTS is not loaded: 503 with instructions (Python 3.9–3.11 required).
    - If no reference: returns tamixa.mp3 (stub).
    """
    use_xtts = _get_tts() is not None
    has_ref = _has_reference(req)

    if has_ref and not use_xtts:
        raise HTTPException(
            status_code=503,
            detail=TTS_REQUIRED_MSG,
        )

    if not has_ref:
        print(
            "[STUB] No reference (base64/url/local path); text length=%s -> returning tamixa.mp3"
            % len(req.text)
        )
        return _stub_response()

    try:
        with _reference_audio_path(
            req.reference_audio_base64, req.reference_url, req.reference_path
        ) as speaker_wav_path:
            wav_bytes, chunk_count = _synthesize_with_xtts(
                req.text, speaker_wav_path, req.language, req.requested_language
            )
        return Response(
            content=wav_bytes,
            media_type="audio/wav",
            headers={
                "Content-Disposition": "inline; filename=\"synthesis.wav\"",
                "X-Synthesis-Status": "completed",
                "X-Synthesis-Bytes": str(len(wav_bytes)),
                "X-Synthesis-Chunks": str(chunk_count),
            },
        )
    except HTTPException as he:
        print("[XTTS] SYNTHESIS_NOT_COMPLETED status=%s detail=%s" % (he.status_code, he.detail))
        raise
    except Exception as e:
        print("[XTTS] SYNTHESIS_NOT_COMPLETED error=%s" % e)
        raise HTTPException(status_code=500, detail="Synthesis failed: %s" % e)


@app.get("/preload")
async def preload():
    """Load the Coqui XTTS model now so the first /synthesize request doesn't wait. Returns whether the model loaded."""
    loaded = _get_tts() is not None
    return {"loaded": loaded}


@app.get("/health")
async def health():
    loaded = _get_tts() is not None
    out = {"status": "ok", "xtts_loaded": loaded}
    if not loaded and _tts_load_error:
        out["xtts_load_error"] = _tts_load_error
    return out
