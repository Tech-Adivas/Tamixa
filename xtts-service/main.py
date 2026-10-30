"""
XTTS-style voice cloning service.

- If Coqui TTS is installed and reference audio is provided (reference_audio_base64 or reference_url),
  synthesizes the story text in the cloned voice and returns WAV audio.
- If reference is provided but TTS is not loaded (e.g. Python 3.13): returns 503 with instructions.
- If no reference: returns tamixa.mp3 (stub).

Backend sends reference_audio_base64 (bytes as base64) and/or reference_url (presigned S3). reference_path is a
storage key label; it is read from disk only in dev when XTTS_LOCAL_REFERENCE_DIR is set, and only for audio
files inside that directory. Reference audio is capped at XTTS_MAX_REFERENCE_BYTES (default 20 MB).
reference_url is fetched only over https from XTTS_REFERENCE_URL_ALLOWED_HOSTS, never from a host that resolves to a
private, loopback or link-local address, without following redirects, and only when the response is audio.

/synthesize and /preload require header X-XTTS-Api-Key matching XTTS_API_KEY (or XTTS_ALLOW_UNAUTHENTICATED=true
for local dev). /health stays open for container health checks.
"""

from __future__ import annotations

import base64
import contextlib
import hmac
import html
import io
import ipaddress
import logging
import os
import re
import socket
import sys
import tempfile
import urllib.error
import urllib.parse
import urllib.request
from contextlib import contextmanager

from fastapi import Depends, FastAPI, Header, HTTPException
from pydantic import BaseModel
from starlette.responses import Response

app = FastAPI(title="XTTS Voice Cloning")

# Structured, single-line logs. Never log reference URLs (presigned URLs carry signatures) or reference audio.
logging.basicConfig(
    level=os.environ.get("XTTS_LOG_LEVEL", "INFO").upper(),
    format="%(asctime)s %(levelname)s %(name)s %(message)s",
)
log = logging.getLogger("xtts")

# Lazy-loaded Coqui TTS model (set on first successful use)
_tts_model = None
_tts_load_error = None  # Last exception message when load failed (so /health can show why)
_XTTS_SAMPLE_RATE = 24000
# Coqui XTTS warns above ~250 chars per call for some languages (e.g. hi); chunk to avoid truncated audio.
_XTTS_CHUNK_MAX_CHARS = 240
# Short silence between chunks (seconds) so concatenation sounds natural
_XTTS_CHUNK_GAP_SEC = 0.12

# Local reference files are a dev-only convenience: disabled unless XTTS_LOCAL_REFERENCE_DIR is set, and then
# only regular (non-symlink) audio files inside that directory are read. reference_path is otherwise just a label.
_LOCAL_REFERENCE_DIR = (os.environ.get("XTTS_LOCAL_REFERENCE_DIR") or "").strip()
_ALLOWED_REFERENCE_EXTENSIONS = {".wav", ".mp3", ".m4a", ".aac", ".ogg", ".flac", ".webm"}
# Max reference audio size for every source (base64, url, local file). Voice samples are well under this.
_MAX_REFERENCE_BYTES = int(os.environ.get("XTTS_MAX_REFERENCE_BYTES") or 20 * 1024 * 1024)

# Shared secret the backend sends as X-XTTS-Api-Key. Without it, /synthesize and /preload refuse requests unless
# XTTS_ALLOW_UNAUTHENTICATED=true (local dev only).
_API_KEY = (os.environ.get("XTTS_API_KEY") or "").strip()
_ALLOW_UNAUTHENTICATED = (os.environ.get("XTTS_ALLOW_UNAUTHENTICATED") or "").strip().lower() == "true"

# reference_url is fetched only from these hosts (comma-separated, e.g. your S3 bucket host). Empty = never fetch;
# the backend sends reference_audio_base64 anyway. Redirects are not followed.
_REFERENCE_URL_ALLOWED_HOSTS = {
    h.strip().lower()
    for h in (os.environ.get("XTTS_REFERENCE_URL_ALLOWED_HOSTS") or "").split(",")
    if h.strip()
}
# Local dev only (e.g. the http://localhost:9090 S3 mock): allow http and private/loopback addresses for reference_url.
_REFERENCE_URL_ALLOW_INSECURE = (
    (os.environ.get("XTTS_REFERENCE_URL_ALLOW_INSECURE") or "").strip().lower() == "true"
)
_REFERENCE_FETCH_TIMEOUT_SEC = 30
# S3 serves uploads as audio/* or, when no type was set, as a generic binary type.
_ALLOWED_REFERENCE_CONTENT_TYPES = ("audio/", "video/mp4", "video/webm", "application/octet-stream", "binary/octet-stream")

TTS_REQUIRED_MSG = (
    "Real story-in-cloned-voice requires Coqui TTS (Python 3.9–3.11). "
    "Use: python3.11 -m venv venv-xtts && source venv-xtts/bin/activate && pip install -r requirements.txt -r requirements-xtts.txt"
)


class SynthesizeRequest(BaseModel):
    text: str
    # Storage key sent by the backend (used for logging); only read from disk under XTTS_LOCAL_REFERENCE_DIR.
    reference_path: str = ""
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
                log.warning("event=model_mps_fallback error=%s", type(e).__name__)
                device = "cpu"
                _tts_model = TTS(model_name).to("cpu")
            else:
                raise
        _tts_load_error = None
        log.info("event=model_loaded device=%s", device)
        return _tts_model
    except Exception as e:
        err_msg = "%s: %s" % (type(e).__name__, e)
        _tts_load_error = err_msg
        log.warning("event=model_unavailable error=%s", err_msg)
        _tts_model = False
        return None


def _ensure_wav(path: str) -> str:
    """Convert MP3 (or other) to WAV so Coqui TTS does not need torchcodec. Returns path to WAV (a new temp file)."""
    if path.lower().endswith(".wav"):
        return path
    try:
        from pydub import AudioSegment
        seg = AudioSegment.from_file(path)
        with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as f:
            wav_path = f.name
        seg.export(wav_path, format="wav")
        return wav_path
    except Exception as e:
        log.warning("event=reference_conversion_failed error=%s", type(e).__name__)
        raise HTTPException(
            status_code=400,
            detail="Reference audio is not WAV and could not be converted (needs pydub + ffmpeg for MP3).",
        )


def require_api_key(x_xtts_api_key: str | None = Header(default=None)) -> None:
    """Reject callers that do not present the shared XTTS_API_KEY (constant-time compare)."""
    if not _API_KEY:
        if _ALLOW_UNAUTHENTICATED:
            return
        raise HTTPException(status_code=503, detail="XTTS_API_KEY is not configured on the service.")
    if not x_xtts_api_key or not hmac.compare_digest(x_xtts_api_key.encode(), _API_KEY.encode()):
        raise HTTPException(status_code=401, detail="Invalid or missing X-XTTS-Api-Key.")


class _NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        raise urllib.error.HTTPError(req.full_url, code, "Redirects are not allowed", headers, fp)


_url_opener = urllib.request.build_opener(_NoRedirect)


def _is_public_address(addr: str) -> bool:
    ip = ipaddress.ip_address(addr.split("%", 1)[0])
    if isinstance(ip, ipaddress.IPv6Address) and ip.ipv4_mapped:
        ip = ip.ipv4_mapped
    return ip.is_global and not ip.is_multicast


def _check_reference_url(reference_url: str) -> None:
    """
    Allow only https URLs (http too when XTTS_REFERENCE_URL_ALLOW_INSECURE) to an allow-listed host, with no embedded
    credentials, whose every resolved address is public (blocks loopback, private, link-local / cloud metadata).
    """
    parsed = urllib.parse.urlsplit(reference_url)
    host = (parsed.hostname or "").lower()
    allowed_schemes = ("http", "https") if _REFERENCE_URL_ALLOW_INSECURE else ("https",)
    if (
        parsed.scheme not in allowed_schemes
        or parsed.username
        or parsed.password
        or not host
        or host not in _REFERENCE_URL_ALLOWED_HOSTS
    ):
        log.warning("event=reference_url_rejected reason=scheme_or_host host=%s", host or "-")
        raise HTTPException(status_code=400, detail="reference_url host is not allowed.")
    if _REFERENCE_URL_ALLOW_INSECURE:
        return
    try:
        infos = socket.getaddrinfo(host, parsed.port or 443, proto=socket.IPPROTO_TCP)
    except (socket.gaierror, UnicodeError):
        log.warning("event=reference_url_rejected reason=dns host=%s", host)
        raise HTTPException(status_code=400, detail="reference_url host could not be resolved.")
    if not infos or not all(_is_public_address(info[4][0]) for info in infos):
        log.warning("event=reference_url_rejected reason=non_public_address host=%s", host)
        raise HTTPException(status_code=400, detail="reference_url host is not allowed.")


def _check_reference_content_type(content_type: str | None) -> None:
    ctype = (content_type or "").split(";", 1)[0].strip().lower()
    if not ctype.startswith(_ALLOWED_REFERENCE_CONTENT_TYPES):
        log.warning("event=reference_url_rejected reason=content_type content_type=%s", ctype or "-")
        raise HTTPException(status_code=415, detail="reference_url did not return audio.")


def _check_reference_size(size: int) -> None:
    if size > _MAX_REFERENCE_BYTES:
        raise HTTPException(
            status_code=413,
            detail="Reference audio too large (max %d bytes)." % _MAX_REFERENCE_BYTES,
        )


def _resolve_local_reference(reference_path: str) -> str | None:
    """
    Return the real path of an allowed local reference file, or None when local references are disabled
    or the path is not a regular audio file inside XTTS_LOCAL_REFERENCE_DIR (no symlinks, no traversal).
    """
    if not _LOCAL_REFERENCE_DIR or not reference_path:
        return None
    base = os.path.realpath(_LOCAL_REFERENCE_DIR)
    candidate = os.path.join(base, reference_path.lstrip("/\\"))
    real = os.path.realpath(candidate)
    try:
        if os.path.commonpath([base, real]) != base:
            return None
    except ValueError:
        return None  # e.g. different drive on Windows
    if os.path.islink(candidate) or not os.path.isfile(real):
        return None
    if os.path.splitext(real)[1].lower() not in _ALLOWED_REFERENCE_EXTENSIONS:
        return None
    return real


def _load_reference_bytes(
    reference_audio_base64: str | None,
    reference_url: str | None,
    reference_path: str,
) -> bytes:
    """Read reference audio: prefer reference_audio_base64, then reference_url, then an allowed local file."""
    if reference_audio_base64:
        # base64 is ~4/3 of the decoded size; reject oversized payloads before decoding
        _check_reference_size(len(reference_audio_base64) * 3 // 4)
        try:
            return base64.b64decode(reference_audio_base64)
        except Exception:
            raise HTTPException(status_code=400, detail="Invalid reference_audio_base64.")
    if reference_url:
        _check_reference_url(reference_url)
        host = urllib.parse.urlsplit(reference_url).hostname
        try:
            req = urllib.request.Request(reference_url, headers={"User-Agent": "TamixaXTTS/1.0"})
            with _url_opener.open(req, timeout=_REFERENCE_FETCH_TIMEOUT_SEC) as resp:
                _check_reference_content_type(resp.headers.get("Content-Type"))
                declared = resp.headers.get("Content-Length")
                if declared and declared.isdigit():
                    _check_reference_size(int(declared))
                data = resp.read(_MAX_REFERENCE_BYTES + 1)
        except HTTPException:
            raise
        except Exception as e:
            # Log only the host and error type/status: the URL and its message may carry a presigned signature.
            status = getattr(e, "code", None)
            log.warning("event=reference_fetch_failed host=%s error=%s status=%s", host, type(e).__name__, status)
            raise HTTPException(status_code=400, detail="Failed to fetch reference_url.")
        _check_reference_size(len(data))
        return data
    local = _resolve_local_reference(reference_path)
    if local:
        _check_reference_size(os.path.getsize(local))
        with open(local, "rb") as f:
            return f.read()
    raise HTTPException(
        status_code=400,
        detail="Need reference_audio_base64 or reference_url.",
    )


@contextmanager
def _reference_audio_path(
    reference_audio_base64: str | None,
    reference_url: str | None,
    reference_path: str,
):
    """
    Yield a path to a temp copy of the reference audio (always WAV so Coqui TTS does not need torchcodec).
    Source files are never converted in place.
    """
    data = _load_reference_bytes(reference_audio_base64, reference_url, reference_path)
    suffix = ".wav" if data[:4] == b"RIFF" else ".mp3"
    with tempfile.NamedTemporaryFile(suffix=suffix, delete=False) as f:
        tmp = f.name
        f.write(data)
    wav_path = tmp
    try:
        wav_path = _ensure_wav(tmp)
        yield wav_path
    finally:
        for p in {tmp, wav_path}:
            try:
                os.unlink(p)
            except OSError:
                pass


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
    log.info(
        "event=synthesis_completed chunks=%d wav_bytes=%d language=%s requested_language=%s",
        n, len(out), language, requested_language or language,
    )
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
        or _resolve_local_reference(req.reference_path)
    )


@app.post("/synthesize", dependencies=[Depends(require_api_key)])
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
        log.info("event=stub_response reason=no_reference text_chars=%d", len(req.text))
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
        log.warning("event=synthesis_not_completed status=%s detail=%s", he.status_code, he.detail)
        raise
    except Exception as e:
        log.error("event=synthesis_not_completed status=500 error=%s", type(e).__name__)
        raise HTTPException(status_code=500, detail="Synthesis failed.")


@app.get("/preload", dependencies=[Depends(require_api_key)])
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
