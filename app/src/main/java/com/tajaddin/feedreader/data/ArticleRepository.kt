package com.tajaddin.feedreader.data

import com.tajaddin.feedreader.data.local.ArticleDao
import com.tajaddin.feedreader.data.remote.FeedApi
import com.tajaddin.feedreader.sync.SyncReconciler
import kotlinx.coroutines.flow.Flow

/**
 * Single source of truth: the UI always reads from the local cache (Room),
 * and refresh() pulls the remote feed, reconciles it against the cache
 * (preserving local read/bookmark state), and writes the merged result back.
 */
class ArticleRepository(
    private val dao: ArticleDao,
    private val api: FeedApi,
) {
    fun observeArticles(): Flow<List<Article>> = dao.observeAll()

    /** Pull remote, then reconcile and persist the merged set in one transaction. */
    suspend fun refresh(): SyncReconciler.Result {
        val remote = api.fetchFeed().map { it.toArticle() }
        return dao.replaceWithReconciled(remote)
    }

    suspend fun setRead(id: String, read: Boolean) = dao.setRead(id, read)
    suspend fun setBookmarked(id: String, bookmarked: Boolean) = dao.setBookmarked(id, bookmarked)
}
