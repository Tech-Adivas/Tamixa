package com.araro.android.player

import android.content.Context
import com.araro.util.AraroLog
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Synthesizes story text to an audio file using TTS.
 * Returns file URI (file://...) or null on failure.
 * ExoPlayer plays the file - avoids TTS speak() playback issues.
 */
suspend fun synthesizeStoryToFile(
    context: Context,
    text: String,
    languageCode: String
): String? {
    if (text.isBlank()) return null
    return trySynthesizeWithLocale(context, text, languageCodeToLocale(languageCode), languageCode)
        ?: if (languageCode.lowercase() != "en") {
            AraroLog.w("TtsSynthesizer", "Primary language failed, retrying with English fallback")
            trySynthesizeWithLocale(context, text, Locale.ENGLISH, "en")
        } else null
}

private suspend fun trySynthesizeWithLocale(
    context: Context,
    text: String,
    locale: Locale,
    languageCode: String
): String? = suspendCancellableCoroutine { cont ->
    val outputFile = File(context.cacheDir, "araro_tts_${System.currentTimeMillis()}.wav")
    val handler = Handler(Looper.getMainLooper())
    var engineRef: TextToSpeech? = null
    var resumed = false
    fun safeResume(value: String?) {
        if (!resumed) {
            resumed = true
            cont.resume(value)
        }
    }

    engineRef = TextToSpeech(context) { status ->
        if (status != TextToSpeech.SUCCESS) {
            AraroLog.w("TtsSynthesizer", "TTS init failed: status=$status")
            engineRef?.shutdown()
            engineRef = null
            safeResume(null)
            return@TextToSpeech
        }
        val engine = engineRef ?: return@TextToSpeech
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            engine.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
        }
        val result = engine.setLanguage(locale)
        val langStatus = when (result) {
            TextToSpeech.LANG_AVAILABLE -> "LANG_AVAILABLE"
            TextToSpeech.LANG_COUNTRY_AVAILABLE -> "LANG_COUNTRY_AVAILABLE"
            TextToSpeech.LANG_MISSING_DATA -> "LANG_MISSING_DATA"
            TextToSpeech.LANG_NOT_SUPPORTED -> "LANG_NOT_SUPPORTED"
            else -> "UNKNOWN($result)"
        }
        AraroLog.d("TtsSynthesizer", "language=$languageCode locale=$locale setLanguage=$langStatus textLen=${text.length}")
        engine.language = locale
        engine.setSpeechRate(0.88f)

        val utteranceId = "araro_synth_${System.currentTimeMillis()}"
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(id: String?) {
                handler.post {
                    engineRef?.shutdown()
                    engineRef = null
                    if (outputFile.exists() && outputFile.length() > 0) {
                        AraroLog.d("TtsSynthesizer", "synthesis OK: ${outputFile.length()} bytes")
                        safeResume(Uri.fromFile(outputFile).toString())
                    } else {
                        AraroLog.w("TtsSynthesizer", "synthesis produced empty file")
                        outputFile.delete()
                        safeResume(null)
                    }
                }
            }
            @Suppress("OVERRIDE_DEPRECATION")
            override fun onError(id: String?) {
                onError(id, -1)
            }
            override fun onError(id: String?, errorCode: Int) {
                handler.post {
                    AraroLog.w("TtsSynthesizer", "synthesis onError: id=$id errorCode=$errorCode language=$languageCode locale=$locale")
                    engineRef?.shutdown()
                    engineRef = null
                    outputFile.delete()
                    safeResume(null)
                }
            }
        })

        val synthResult = engine.synthesizeToFile(text, android.os.Bundle(), outputFile, utteranceId)
        if (synthResult != TextToSpeech.SUCCESS) {
            AraroLog.w("TtsSynthesizer", "synthesizeToFile returned $synthResult (expected SUCCESS)")
            engine.shutdown()
            engineRef = null
            outputFile.delete()
            safeResume(null)
        }
    }
    cont.invokeOnCancellation {
        engineRef?.shutdown()
        outputFile.delete()
    }
}

private fun languageCodeToLocale(code: String): Locale = when (code.lowercase()) {
    "ta" -> Locale("ta", "IN")
    "hi" -> Locale("hi", "IN")
    "te" -> Locale("te", "IN")
    "kn" -> Locale("kn", "IN")
    "ml" -> Locale("ml", "IN")
    "mr" -> Locale("mr", "IN")
    "bn" -> Locale("bn", "IN")
    "en" -> Locale.ENGLISH
    else -> {
        val parts = code.split("-", "_")
        if (parts.size >= 2) Locale(parts[0], parts[1])
        else Locale.forLanguageTag(code)
    }
}
