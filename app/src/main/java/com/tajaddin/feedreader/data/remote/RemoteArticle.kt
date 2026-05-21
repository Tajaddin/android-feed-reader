package com.tajaddin.feedreader.data.remote

import com.tajaddin.feedreader.data.Article
import kotlinx.serialization.Serializable

/** Network DTO. Mapped to the domain Article at the boundary. */
@Serializable
data class RemoteArticle(
    val id: String,
    val title: String,
    val summary: String = "",
    val url: String,
    val publishedAtMs: Long,
) {
    fun toArticle(): Article = Article(
        id = id,
        title = title,
        summary = summary,
        url = url,
        publishedAtMs = publishedAtMs,
    )
}
