package com.tamixa.network

import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.darwin.Darwin

/** iOS Ktor engine (Darwin). Certificate pinning was removed — reintroduce with correct Darwin/Kotlin-Native interop when fingerprints are real. */
actual val ktorEngine: HttpClientEngineFactory<HttpClientEngineConfig> = Darwin
