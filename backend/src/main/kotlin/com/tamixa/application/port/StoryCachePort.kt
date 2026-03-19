package com.tamixa.application.port

interface StoryCachePort {

    fun get(cacheKey: String): String?

    fun set(cacheKey: String, content: String)
}
