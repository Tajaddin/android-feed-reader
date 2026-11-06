package com.tajaddin.feedreader.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.tajaddin.feedreader.data.Article
import com.tajaddin.feedreader.sync.SyncReconciler
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

    /**
     * Reconciles [remote] with the cache and replaces the cache with the merged
     * set in one transaction. A refresh that fails or is cancelled part-way rolls
     * back instead of leaving the cache cleared, and a read/bookmark flag written
     * meanwhile waits for the commit instead of landing between the read and the
     * rewrite, where it would be lost.
     */
    @Transaction
    suspend fun replaceWithReconciled(remote: List<Article>): SyncReconciler.Result {
        val result = SyncReconciler.reconcile(getAll(), remote)
        clear()
        upsertAll(result.merged)
        return result
    }
}
