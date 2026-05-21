package com.tajaddin.feedreader.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Single model used across the app and persisted by Room. Content fields come
 * from the remote feed; isRead and isBookmarked are local user state that the
 * sync must never clobber.
 */
@Entity(tableName = "articles")
data class Article(
    @PrimaryKey val id: String,
    val title: String,
    val summary: String,
    val url: String,
    val publishedAtMs: Long,
    val isRead: Boolean = false,
    val isBookmarked: Boolean = false,
)
