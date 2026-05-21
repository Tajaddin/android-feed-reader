package com.tajaddin.feedreader

import com.tajaddin.feedreader.data.Article
import com.tajaddin.feedreader.data.ArticleRepository
import com.tajaddin.feedreader.data.local.ArticleDao
import com.tajaddin.feedreader.data.remote.FeedApi
import com.tajaddin.feedreader.data.remote.RemoteArticle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** In-memory fake DAO so the repository is tested without Room/Android. */
class FakeArticleDao : ArticleDao {
    private val state = MutableStateFlow<List<Article>>(emptyList())
    override fun observeAll(): Flow<List<Article>> = state
    override suspend fun getAll(): List<Article> = state.value.sortedByDescending { it.publishedAtMs }
    override suspend fun upsertAll(articles: List<Article>) {
        val byId = state.value.associateBy { it.id }.toMutableMap()
        for (a in articles) byId[a.id] = a
        state.value = byId.values.toList()
    }
    override suspend fun clear() { state.value = emptyList() }
    override suspend fun setRead(id: String, read: Boolean) {
        state.value = state.value.map { if (it.id == id) it.copy(isRead = read) else it }
    }
    override suspend fun setBookmarked(id: String, bookmarked: Boolean) {
        state.value = state.value.map { if (it.id == id) it.copy(isBookmarked = bookmarked) else it }
    }
}

class FakeFeedApi(var feed: List<RemoteArticle>) : FeedApi {
    var calls = 0
    override suspend fun fetchFeed(): List<RemoteArticle> { calls++; return feed }
}

class ArticleRepositoryTest {

    private fun remote(id: String, pub: Long = 0) =
        RemoteArticle(id = id, title = "t$id", summary = "s", url = "u$id", publishedAtMs = pub)

    @Test
    fun `refresh populates the cache from remote`() = runTest {
        val dao = FakeArticleDao()
        val api = FakeFeedApi(listOf(remote("1"), remote("2")))
        val repo = ArticleRepository(dao, api)
        val result = repo.refresh()
        assertEquals(2, result.added)
        assertEquals(2, dao.getAll().size)
    }

    @Test
    fun `refresh preserves a locally read article across syncs`() = runTest {
        val dao = FakeArticleDao()
        val api = FakeFeedApi(listOf(remote("1")))
        val repo = ArticleRepository(dao, api)
        repo.refresh()
        repo.setRead("1", true)
        // second sync re-sends the same article with no flags
        repo.refresh()
        assertTrue(dao.getAll().single().isRead)
    }

    @Test
    fun `bookmarked article survives when it drops out of the feed`() = runTest {
        val dao = FakeArticleDao()
        val api = FakeFeedApi(listOf(remote("1")))
        val repo = ArticleRepository(dao, api)
        repo.refresh()
        repo.setBookmarked("1", true)
        api.feed = listOf(remote("2")) // article 1 no longer in feed
        repo.refresh()
        val ids = dao.getAll().map { it.id }.toSet()
        assertTrue("bookmarked kept", ids.contains("1"))
        assertTrue("new article added", ids.contains("2"))
    }

    @Test
    fun `non-bookmarked dropped article is evicted`() = runTest {
        val dao = FakeArticleDao()
        val api = FakeFeedApi(listOf(remote("1")))
        val repo = ArticleRepository(dao, api)
        repo.refresh()
        api.feed = listOf(remote("2"))
        repo.refresh()
        assertFalse(dao.getAll().any { it.id == "1" })
    }

    @Test
    fun `toggle helpers update the cache`() = runTest {
        val dao = FakeArticleDao()
        val repo = ArticleRepository(dao, FakeFeedApi(listOf(remote("1"))))
        repo.refresh()
        repo.setRead("1", true)
        repo.setBookmarked("1", true)
        val a = dao.getAll().single()
        assertTrue(a.isRead && a.isBookmarked)
    }
}
