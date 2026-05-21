package com.tajaddin.feedreader.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** Remote feed source. The repository depends on this interface, so tests
 *  supply a fake and never touch the network. */
interface FeedApi {
    suspend fun fetchFeed(): List<RemoteArticle>
}

class KtorFeedApi(
    private val baseUrl: String,
    private val client: HttpClient = HttpClient {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    },
) : FeedApi {
    override suspend fun fetchFeed(): List<RemoteArticle> =
        client.get("$baseUrl/feed.json").body()
}
