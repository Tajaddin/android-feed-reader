package com.tajaddin.feedreader.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tajaddin.feedreader.data.Article
import kotlinx.coroutines.flow.Flow

/** Local cache access. Room generates the implementation; tests use a fake. */
@Dao
interface ArticleDao {
    @Query("SELECT * FROM articles ORDER BY publishedAtMs DESC")
    fun observeAll(): Flow<List<Article>>

    @Query("SELECT * FROM articles ORDER BY publishedAtMs DESC")
    suspend fun getAll(): List<Article>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(articles: List<Article>)

    @Query("DELETE FROM articles")
    suspend fun clear()

    @Query("UPDATE articles SET isRead = :read WHERE id = :id")
    suspend fun setRead(id: String, read: Boolean)

    @Query("UPDATE articles SET isBookmarked = :bookmarked WHERE id = :id")
    suspend fun setBookmarked(id: String, bookmarked: Boolean)
}
