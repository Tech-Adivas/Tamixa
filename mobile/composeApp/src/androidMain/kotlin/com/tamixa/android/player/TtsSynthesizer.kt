package com.tamixa.android.player

import android.content.Context
import com.tamixa.util.TamixaLog
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

/** Android TTS has ~4000 character limit per synthesizeToFile call. Chunk at sentence boundaries. */
private const val MAX_CHARS_PER_CHUNK = 3500

/**
 * Synthesizes story text to an audio file using TTS.
 * Chunks text to work around Android's 4000-char limit so full stories play to the end.
 * Returns file URI (file://...) or null on failure.
 */
suspend fun synthesizeStoryToFile(
    context: Context,
    text: String,
    languageCode: String
): String? {
    if (text.isBlank()) return null
    val locale = languageCodeToLocale(languageCode)
    val chunks = chunkTextForTts(text.trim(), MAX_CHARS_PER_CHUNK)
    if (chunks.isEmpty()) return null

    val result = if (chunks.size == 1) {
        trySynthesizeWithLocale(context, text.trim(), locale, languageCode)
    } else {
        TamixaLog.d("TtsSynthesizer", "Chunking ${text.length} chars into ${chunks.size} parts for full synthesis")
        synthesizeChunked(context, chunks, locale, languageCode)
    }
    return result ?: if (languageCode.lowercase() != "en") {
        TamixaLog.w("TtsSynthesizer", "Primary language failed, retrying with English fallback")
        val enChunks = chunkTextForTts(text.trim(), MAX_CHARS_PER_CHUNK)
        if (enChunks.size == 1) trySynthesizeWithLocale(context, text.trim(), Locale.ENGLISH, "en")
        else synthesizeChunked(context, enChunks, Locale.ENGLISH, "en")
    } else null
}

/** Split text at sentence boundaries, max maxChars per chunk. */
private fun chunkTextForTts(text: String, maxChars: Int): List<String> {
    if (text.length <= maxChars) return listOf(text)
    val result = mutableListOf<String>()
    var start = 0
    while (start < text.length) {
        var end = (start + maxChars).coerceAtMost(text.length)
        if (end < text.length) {
            val segment = text.substring(start, end)
            val lastPeriod = segment.lastIndexOf('.')
            val lastQuestion = segment.lastIndexOf('?')
            val lastExclaim = segment.lastIndexOf('!')
            val breakPoint = maxOf(lastPeriod, lastQuestion, lastExclaim)
            if (breakPoint >= 0) end = start + breakPoint + 1
            else {
                val lastSpace = segment.lastIndexOf(' ')
                if (lastSpace >= 0) end = start + lastSpace + 1
            }
        }
        val chunk = text.substring(start, end).trim()
        if (chunk.isNotBlank()) result.add(chunk)
        start = end
    }
    return result
}

private suspend fun synthesizeChunked(
    context: Context,
    chunks: List<String>,
    locale: Locale,
    languageCode: String
): String? {
    val tempFiles = mutableListOf<File>()
    for ((i, chunk) in chunks.withIndex()) {
        val file = File(context.cacheDir, "tamixa_tts_chunk_${i}_${System.currentTimeMillis()}.wav")
        val ok = trySynthesizeChunkToFile(context, chunk, locale, languageCode, file)
        if (!ok) {
            tempFiles.forEach { it.delete() }
            return null
        }
        tempFiles.add(file)
    }
    val outputFile = File(context.cacheDir, "tamixa_tts_${System.currentTimeMillis()}.wav")
    return try {
        concatenateWavFiles(tempFiles, outputFile)
        tempFiles.forEach { it.delete() }
        Uri.fromFile(outputFile).toString()
    } catch (e: Exception) {
        TamixaLog.w("TtsSynthesizer", "WAV concatenation failed", e)
        tempFiles.forEach { it.delete() }
        outputFile.delete()
        null
    }
}

/** Concatenate WAV files. Assumes same format; uses first file's header. */
private fun concatenateWavFiles(inputFiles: List<File>, outputFile: File) {
    if (inputFiles.isEmpty()) return
    if (inputFiles.size == 1) {
        inputFiles[0].copyTo(outputFile, overwrite = true)
        return
    }
    val first = inputFiles[0]
    val header = first.readBytes().take(44).toByteArray()
    var totalDataSize = 0L
    for (f in inputFiles) {
        val size = f.length()
        if (size > 44) totalDataSize += size - 44
    }
    // WAV: bytes 4-7 = riff chunk size (file size - 8), bytes 40-43 = data chunk size
    val fileSize = 36 + totalDataSize
    header[4] = (fileSize and 0xFF).toByte()
    header[5] = ((fileSize shr 8) and 0xFF).toByte()
    header[6] = ((fileSize shr 16) and 0xFF).toByte()
    header[7] = ((fileSize shr 24) and 0xFF).toByte()
    val dataSize = totalDataSize.toInt()
    header[40] = (dataSize and 0xFF).toByte()
    header[41] = ((dataSize shr 8) and 0xFF).toByte()
    header[42] = ((dataSize shr 16) and 0xFF).toByte()
    header[43] = ((dataSize shr 24) and 0xFF).toByte()

    outputFile.outputStream().use { out ->
        out.write(header)
        for (f in inputFiles) {
            f.inputStream().use { inp ->
                inp.skip(44)
                inp.copyTo(out)
            }
        }
    }
}

private suspend fun trySynthesizeChunkToFile(
    context: Context,
    text: String,
    locale: Locale,
    languageCode: String,
    outputFile: File
): Boolean = suspendCancellableCoroutine { cont ->
    val handler = Handler(Looper.getMainLooper())
    var engineRef: TextToSpeech? = null
    var resumed = false
    fun safeResume(value: Boolean) {
        if (!resumed) {
            resumed = true
            cont.resume(value)
        }
    }

    engineRef = TextToSpeech(context) { status ->
        if (status != TextToSpeech.SUCCESS) {
            engineRef?.shutdown()
            engineRef = null
            outputFile.delete()
            safeResume(false)
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
        engine.setLanguage(locale)
        engine.language = locale
        engine.setSpeechRate(0.88f)
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(id: String?) {
                handler.post {
                    engineRef?.shutdown()
                    engineRef = null
                    safeResume(outputFile.exists() && outputFile.length() > 0)
                }
            }
            @Suppress("OVERRIDE_DEPRECATION")
            override fun onError(id: String?) { onError(id, -1) }
            override fun onError(id: String?, errorCode: Int) {
                handler.post {
                    engineRef?.shutdown()
                    engineRef = null
                    outputFile.delete()
                    safeResume(false)
                }
            }
        })
        val utteranceId = "tamixa_synth_${System.currentTimeMillis()}"
        val synthResult = engine.synthesizeToFile(text, android.os.Bundle(), outputFile, utteranceId)
        if (synthResult != TextToSpeech.SUCCESS) {
            engine.shutdown()
            engineRef = null
            outputFile.delete()
            safeResume(false)
        }
    }
    cont.invokeOnCancellation {
        engineRef?.shutdown()
        outputFile.delete()
    }
}

private suspend fun trySynthesizeWithLocale(
    context: Context,
    text: String,
    locale: Locale,
    languageCode: String
): String? = suspendCancellableCoroutine { cont ->
    val outputFile = File(context.cacheDir, "tamixa_tts_${System.currentTimeMillis()}.wav")
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
            TamixaLog.w("TtsSynthesizer", "TTS init failed: status=$status")
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
        engine.setLanguage(locale)
        engine.language = locale
        engine.setSpeechRate(0.88f)

        val utteranceId = "tamixa_synth_${System.currentTimeMillis()}"
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(id: String?) {
                handler.post {
                    engineRef?.shutdown()
                    engineRef = null
                    if (outputFile.exists() && outputFile.length() > 0) {
                        TamixaLog.d("TtsSynthesizer", "synthesis OK: ${outputFile.length()} bytes")
                        safeResume(Uri.fromFile(outputFile).toString())
                    } else {
                        TamixaLog.w("TtsSynthesizer", "synthesis produced empty file")
                        outputFile.delete()
                        safeResume(null)
                    }
                }
            }
            @Suppress("OVERRIDE_DEPRECATION")
            override fun onError(id: String?) { onError(id, -1) }
            override fun onError(id: String?, errorCode: Int) {
                handler.post {
                    TamixaLog.w("TtsSynthesizer", "synthesis onError: id=$id errorCode=$errorCode")
                    engineRef?.shutdown()
                    engineRef = null
                    outputFile.delete()
                    safeResume(null)
                }
            }
        })

        val synthResult = engine.synthesizeToFile(text, android.os.Bundle(), outputFile, utteranceId)
        if (synthResult != TextToSpeech.SUCCESS) {
            TamixaLog.w("TtsSynthesizer", "synthesizeToFile returned $synthResult")
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
