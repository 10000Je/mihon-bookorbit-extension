package app.bookorbit.mihonext.repository

import app.bookorbit.mihonext.api.ApiFailure
import app.bookorbit.mihonext.api.FailureKind
import app.bookorbit.mihonext.model.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class FilteredPagerTest {
    private fun book(id: Long, format: String, series: Long? = null, status: String = "present") =
        BookCard(id, title = "Book $id", status = status, seriesId = series,
            files = listOf(ComicFile(id * 10, format, "primary")))

    @Test fun epubFirstPageAndEmptyGapsDoNotHideLaterComicsOrInventNextPage() = runTest {
        val calls = ArrayList<Int>()
        val pages = listOf(
            listOf(book(1, "epub"), book(2, "pdf")),
            listOf(book(3, ".CBZ"), book(4, "epub")),
            listOf(book(5, "epub"), book(6, "cbr", status = "missing")),
            listOf(book(7, "CB7"), book(8, "cbr")),
            listOf(book(9, "epub")),
        )
        val pager = FilteredPager({ query, index ->
            assertEquals("찾기", query); calls += index
            BooksPage(pages[index], 9, index, 2)
        })
        val first = pager.page("찾기", 1)
        assertEquals(listOf(3L), first.items.map { it.id }); assertTrue(first.hasNextPage)
        val second = pager.page("찾기", 2)
        assertEquals(listOf(7L, 8L), second.items.map { it.id }); assertFalse(second.hasNextPage)
        assertEquals((0..4).toList(), calls)
        assertEquals(second, pager.page("찾기", 2)); assertEquals(5, calls.size)
    }

    @Test fun duplicateSeriesAcrossPagesAndWithinPageAreNotRepeated() = runTest {
        val pager = FilteredPager({ _, index ->
            BooksPage(if (index == 0) listOf(book(1, "cbz", 20), book(2, "cbr", 20))
                else listOf(book(3, "cb7", 20), book(4, "cbz", 21)), 4, index, 2)
        })
        assertEquals(listOf(20L), pager.page("", 1).items.map { it.seriesId })
        val last = pager.page("", 2)
        assertEquals(listOf(21L), last.items.map { it.seriesId }); assertFalse(last.hasNextPage)
    }

    @Test fun failedLookaheadDoesNotAdvanceTheCursor() = runTest {
        var fail = true
        val pager = FilteredPager({ _, index ->
            if (index == 2 && fail) { fail = false; throw ApiFailure(FailureKind.SERVER, "test") }
            BooksPage(listOf(book(index + 1L, "cbz")), 4, index, 1)
        })
        assertEquals(1L, pager.page("", 1).items.single().id)
        assertThrows(ApiFailure::class.java) { kotlinx.coroutines.runBlocking { pager.page("", 2) } }
        assertEquals(2L, pager.page("", 2).items.single().id)
        assertEquals(3L, pager.page("", 3).items.single().id)
        assertFalse(pager.page("", 4).hasNextPage)
    }

    @Test fun queryIsolationAndCacheExpiryRequireRestartInsteadOfSkippingItems() = runTest {
        var clock = 0L
        val pager = FilteredPager({ q, index -> BooksPage(listOf(book(if (q == "a") 1 else 2, "cbz")), 1, index, 1) }, { clock })
        assertEquals(1L, pager.page("a", 1).items.single().id)
        assertEquals(2L, pager.page("b", 1).items.single().id)
        clock = 300_001
        assertThrows(ApiFailure::class.java) { kotlinx.coroutines.runBlocking { pager.page("a", 2) } }
    }

    @Test fun inconsistentPaginationIsAnErrorAndLookaheadIsBounded() = runTest {
        val inconsistent = FilteredPager({ _, index -> BooksPage(emptyList(), 10, index, 2) })
        assertThrows(ApiFailure::class.java) { kotlinx.coroutines.runBlocking { inconsistent.page("", 1) } }
        var calls = 0
        val endless = FilteredPager({ _, index -> calls++; BooksPage(listOf(book(index + 1L, "epub")), 10_000, index, 1) })
        assertThrows(ApiFailure::class.java) { kotlinx.coroutines.runBlocking { endless.page("", 1) } }
        assertEquals(128, calls)
    }

    @Test fun thousandSeriesUseServerPagesAndOnePageLookahead() = runTest {
        var calls = 0
        val pager = FilteredPager({ _, index -> calls++
            BooksPage((1..40).map { book(index * 40L + it, "cbz") }, 1000, index, 40)
        })
        val ids = (1..25).flatMap { page ->
            val result = pager.page("", page)
            assertEquals(page < 25, result.hasNextPage)
            result.items.map { it.id }
        }
        assertEquals((1L..1000L).toList(), ids); assertEquals(25, calls)
    }
}
