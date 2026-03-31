package com.tamixa.network

import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.request.*

@kotlinx.serialization.Serializable
data class ShortContentResponseDto(
    val id: Long,
    val type: String,
    val content: String,
    val answer: String? = null,
    val language: String,
    val ageMin: Int? = null,
    val ageMax: Int? = null,
    val displayDate: String? = null,
    val audioUrl: String? = null,
    val createdAt: String
)

class ShortContentApi(private val client: HttpClient) {

    /** List short content by type and language (paginated). */
    suspend fun list(type: String, language: String, page: Int = 0, size: Int = 20): List<ShortContentResponseDto> {
        return client.get("${ApiConfig.API_VERSION}/short-content") {
            parameter("type", type)
            parameter("language", language)
            parameter("page", page)
            parameter("size", size)
        }.body()
    }

    /** Get the daily item for a type. */
    suspend fun getDaily(type: String, language: String, date: String? = null): ShortContentResponseDto? {
        return try {
            client.get("${ApiConfig.API_VERSION}/short-content/daily") {
                parameter("type", type)
                parameter("language", language)
                date?.let { parameter("date", it) }
            }.body()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            com.tamixa.util.TamixaLog.w("ShortContentApi", "getDaily failed type=$type lang=$language", e)
            null
        }
    }

    /** Get all supported short content types. */
    suspend fun getTypes(): List<String> {
        return client.get("${ApiConfig.API_VERSION}/short-content/types").body()
    }
}
