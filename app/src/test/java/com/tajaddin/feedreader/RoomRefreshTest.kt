package com.tajaddin.feedreader

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tajaddin.feedreader.data.ArticleRepository
import com.tajaddin.feedreader.data.local.AppDatabase
import com.tajaddin.feedreader.data.remote.RemoteArticle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Repository against a real in-memory Room database, for behaviour the fake DAO cannot model. */
@RunWith(RobolectricTestRunner::class)
class RoomRefreshTest {

    private lateinit var db: AppDatabase

    @Before
    fun openDb() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() = db.close()

    private fun remote(id: String) =
        RemoteArticle(id = id, title = "t$id", summary = "s", url = "u$id", publishedAtMs = 0)

    @Test
    fun `refresh that fails while rewriting the cache leaves it and its bookmarks intact`() = runTest {
        val dao = db.articleDao()
        val api = FakeFeedApi(listOf(remote("1"), remote("2")))
        val repo = ArticleRepository(dao, api)
        repo.refresh()
        repo.setBookmarked("1", true)

        // Fail the insert of one incoming row, after the old rows have been deleted.
        db.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER reject_poison BEFORE INSERT ON articles WHEN NEW.id = 'poison' " +
                "BEGIN SELECT RAISE(ABORT, 'rejected'); END"
        )
        api.feed = listOf(remote("2"), remote("poison"))

        assertTrue(runCatching { repo.refresh() }.isFailure)
        val cached = dao.getAll()
        assertEquals(setOf("1", "2"), cached.map { it.id }.toSet())
        assertTrue(cached.single { it.id == "1" }.isBookmarked)
    }
}
