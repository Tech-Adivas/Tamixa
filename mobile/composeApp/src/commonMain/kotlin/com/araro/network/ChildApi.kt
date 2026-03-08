package com.araro.network

import com.araro.domain.Child
import com.araro.domain.CreateChildRequest
import com.araro.domain.UpdateChildRequest
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.request.*

class ChildApi(private val client: HttpClient) {
    suspend fun create(request: CreateChildRequest): Child =
        client.post("${ApiConfig.API_VERSION}/children") {
            setBody(request)
        }.body()

    suspend fun list(): List<Child> =
        client.get("${ApiConfig.API_VERSION}/children").body()

    suspend fun getById(id: Long): Child =
        client.get("${ApiConfig.API_VERSION}/children/$id").body()

    suspend fun update(id: Long, request: UpdateChildRequest): Child =
        client.patch("${ApiConfig.API_VERSION}/children/$id") {
            setBody(request)
        }.body()
}
