package com.tamixa.network

import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory

/** Platform-specific Ktor engine: CIO on Android, Darwin on iOS (fixes JSON deserialization on Native). */
expect val ktorEngine: HttpClientEngineFactory<HttpClientEngineConfig>
