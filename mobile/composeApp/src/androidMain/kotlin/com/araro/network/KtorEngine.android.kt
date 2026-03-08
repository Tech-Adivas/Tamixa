package com.araro.network

import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.cio.CIO

actual val ktorEngine: HttpClientEngineFactory<HttpClientEngineConfig> = CIO
