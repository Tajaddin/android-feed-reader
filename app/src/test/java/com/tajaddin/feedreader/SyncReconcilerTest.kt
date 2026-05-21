package com.tajaddin.feedreader

import com.tajaddin.feedreader.data.Article
import com.tajaddin.feedreader.sync.SyncReconciler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SyncReconcilerTest {

    private fun art(id: String, read: Boolean = false, bm: Boolean = false, pub: Long = 0) =
        Article(id = id, title = "t$id", summary = "s", url = "u$id", publishedAtMs = pub, isRead = read, isBookmarked = bm)

    @Test
    fun `adds new remote articles`() {
        val r = SyncReconciler.reconcile(emptyList(), listOf(art("1"), art("2")))
        assertEquals(2, r.added)
        assertEquals(2, r.merged.size)
    }

    @Test
    fun `preserves local read and bookmark flags on overlap`() {
        val local = listOf(art("1", read = true, bm = true))
        val remote = listOf(art("1")) // remote has no flags
        val r = SyncReconciler.reconcile(local, remote)
        val merged = r.merged.single()
        assertTrue(merged.isRead)
        assertTrue(merged.isBookmarked)
        assertEquals(1, r.updated)
        assertEquals(1, r.flagsPreserved)
    }

    @Test
    fun `takes remote content but keeps local flags`() {
        val local = listOf(art("1", read = true).copy(title = "OLD"))
        val remote = listOf(art("1").copy(title = "NEW", summary = "fresh"))
        val merged = SyncReconciler.reconcile(local, remote).merged.single()
        assertEquals("NEW", merged.title)
        assertEquals("fresh", merged.summary)
        assertTrue(merged.isRead) // local flag survived
    }

    @Test
    fun `drops local-only articles that are not bookmarked`() {
        val local = listOf(art("gone"))
        val r = SyncReconciler.reconcile(local, listOf(art("1")))
        assertEquals(1, r.removed)
        assertNull(r.merged.find { it.id == "gone" })
    }

    @Test
    fun `keeps bookmarked local articles even if absent from remote`() {
        val local = listOf(art("kept", bm = true))
        val r = SyncReconciler.reconcile(local, listOf(art("1")))
        assertEquals(0, r.removed)
        assertTrue(r.merged.any { it.id == "kept" })
    }

    @Test
    fun `sorts merged newest first`() {
        val r = SyncReconciler.reconcile(emptyList(), listOf(art("a", pub = 10), art("b", pub = 30), art("c", pub = 20)))
        assertEquals(listOf("b", "c", "a"), r.merged.map { it.id })
    }

    // Hero property: across a large reconcile, every local flag is preserved
    // and no bookmark is ever lost. Also a rough perf gate.
    @Test
    fun `preserves all flags across 50k reconcile under 250ms`() {
        val rng = Random(7)
        val local = (0 until 50_000).map { i ->
            art("id$i", read = rng.nextBoolean(), bm = rng.nextInt(10) == 0, pub = i.toLong())
        }
        val expectedFlagged = local.count { it.isRead || it.isBookmarked }
        val expectedBookmarked = local.count { it.isBookmarked }

        // Remote re-sends every id (content only, no flags), drops none.
        val remote = local.map { art(it.id, pub = it.publishedAtMs).copy(title = "fresh-${it.id}") }

        val start = System.nanoTime()
        val r = SyncReconciler.reconcile(local, remote)
        val ms = (System.nanoTime() - start) / 1_000_000.0

        assertEquals(50_000, r.merged.size)
        assertEquals(expectedFlagged, r.flagsPreserved)
        // every read/bookmark flag survived
        val mergedById = r.merged.associateBy { it.id }
        var preservedReads = 0
        var preservedBookmarks = 0
        for (l in local) {
            val m = mergedById[l.id]!!
            if (l.isRead) { assertTrue(m.isRead); preservedReads++ }
            if (l.isBookmarked) { assertTrue(m.isBookmarked); preservedBookmarks++ }
            assertTrue(m.title.startsWith("fresh-")) // remote content applied
        }
        assertEquals(expectedBookmarked, preservedBookmarks)
        assertTrue("preserved reads should be > 0", preservedReads > 0)
        assertTrue("reconcile took ${ms}ms, expected < 250ms", ms < 250.0)
    }
}
