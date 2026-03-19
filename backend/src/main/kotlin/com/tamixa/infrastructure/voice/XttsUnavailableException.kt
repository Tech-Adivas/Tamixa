package com.tamixa.infrastructure.voice

/**
 * Thrown when the XTTS service returns 503 (e.g. Coqui TTS not installed; Python 3.9–3.11 required).
 * Propagates the service's detail message to the API response.
 */
class XttsUnavailableException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
