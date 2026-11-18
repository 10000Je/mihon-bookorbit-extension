package app.bookorbit.mihonext.repository

import app.bookorbit.mihonext.api.ApiFailure
import app.bookorbit.mihonext.api.FailureKind
import app.bookorbit.mihonext.model.*
import app.bookorbit.mihonext.settings.Connection
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/** Local synthetic catalogue pages; no real server or emulator is used. */
class LibraryCataloguePagerTest {
    private val first = Connection("https://books.example/", "key", 1, 7, "A", "a".repeat(32))
    private val second = first.copy(libraryId = 8, libraryName = "B", binding = "b".repeat(32))
    private fun card(id: Long, library: Long) = BookCard(id, library, "Book $id", "present",
        seriesId = id, files = listOf(ComicFile(id, "cbz", "primary")))

    @Test fun allLibrariesSpanPageBoundariesWithoutLosingBooksWithSharedSeriesIds() = runBlocking {
        val calls = mutableListOf<Pair<Long, Int>>()
        val pager = LibraryCataloguePager(load = { connection, _, page ->
            calls += connection.libraryId to page
            assertEquals(1, page)
            CataloguePage((1L..25L).map { card(it, connection.libraryId) }, false)
        })
        val page1 = pager.page(listOf(second, first), "", 1)
        assertEquals(40, page1.items.size)
        assertEquals(List(25) { 7L } + List(15) { 8L }, page1.items.map { it.connection.libraryId })
        assertTrue(page1.hasNextPage)
        val page2 = pager.page(listOf(first, second), "", 2)
        assertEquals((16L..25L).toList(), page2.items.map { it.book.id })
        assertFalse(page2.hasNextPage)
        assertEquals(50, (page1.items + page2.items).map { it.identity }.toSet().size)
        assertEquals(listOf(7L to 1, 8L to 1), calls)
    }

    @Test fun allLibrariesContinueWithinALibraryBeforeAdvancingAndSkipEmptyLibraries() = runBlocking {
        val empty = first.copy(libraryId = 6, binding = "c".repeat(32))
        val calls = mutableListOf<Pair<Long, Int>>()
        val pager = LibraryCataloguePager(load = { connection, _, page ->
            calls += connection.libraryId to page
            when (connection.libraryId) {
                6L -> CataloguePage(emptyList(), false)
                7L -> CataloguePage((if (page == 1) 1L..40L else 41L..45L).map { card(it, 7) }, page == 1)
                else -> CataloguePage(listOf(card(1, 8)), false)
            }
        })
        val page1 = pager.page(listOf(first, second, empty), "", 1)
        val page2 = pager.page(listOf(first, second, empty), "", 2)
        assertTrue(page1.hasNextPage)
        assertEquals(listOf(7L, 7L, 7L, 7L, 7L, 8L), page2.items.map { it.connection.libraryId })
        assertFalse(page2.hasNextPage)
        assertEquals(listOf(6L to 1, 7L to 1, 7L to 2, 8L to 1), calls)
    }

    @Test fun anEmptyLastLibraryDoesNotAdvertiseANextPage() = runBlocking {
        val pager = LibraryCataloguePager(load = { connection, _, _ ->
            CataloguePage(if (connection.libraryId == 7L) (1L..40L).map { card(it, 7) } else emptyList(), false)
        })
        val page = pager.page(listOf(first, second), "", 1)
        assertEquals(40, page.items.size)
        assertFalse(page.hasNextPage)
    }

    @Test fun singleLibraryFilterAndQueriesAreIsolatedFromTheAllLibraryCursor() = runBlocking {
        val requests = mutableListOf<Pair<Long, String>>()
        val pager = LibraryCataloguePager(load = { connection, query, _ ->
            requests += connection.libraryId to query
            CataloguePage(listOf(card(1, connection.libraryId)), false)
        })
        assertEquals(listOf(7L, 8L), pager.page(listOf(first, second), "cat", 1).items.map { it.connection.libraryId })
        assertEquals(listOf(8L), pager.page(listOf(second), "dog", 1).items.map { it.connection.libraryId })
        assertEquals(listOf(7L to "cat", 8L to "cat", 8L to "dog"), requests)
    }

    @Test fun lookAheadFailuresDoNotHideLibrariesOrCommitAPartialCursor() = runBlocking {
        var fail = true
        val pager = LibraryCataloguePager(load = { connection, _, _ ->
            if (connection.libraryId == 8L && fail) throw ApiFailure.http(403)
            CataloguePage((1L..40L).map { card(it, connection.libraryId) }, false)
        })
        val error = assertThrows(ApiFailure::class.java) { runBlocking { pager.page(listOf(first, second), "", 1) } }
        assertEquals(FailureKind.PERMISSION, error.kind)
        fail = false
        val page1 = pager.page(listOf(first, second), "", 1)
        val page2 = pager.page(listOf(first, second), "", 2)
        assertEquals(List(40) { 7L }, page1.items.map { it.connection.libraryId })
        assertEquals(List(40) { 8L }, page2.items.map { it.connection.libraryId })
        assertFalse(page2.hasNextPage)
    }

    @Test fun duplicateAndUnreadableBooksAreRemovedWithinTheirOwnLibrary() = runBlocking {
        val pager = LibraryCataloguePager(load = { connection, _, _ ->
            val good = card(1, connection.libraryId)
            val epub = card(2, connection.libraryId).copy(files = listOf(ComicFile(2, "epub", "primary")))
            CataloguePage(listOf(good, good, epub), false)
        })
        val result = pager.page(listOf(first, second), "", 1)
        assertEquals(listOf(7L, 8L), result.items.map { it.connection.libraryId })
    }

    @Test fun noAccessibleLibrariesReturnsEmptyWithoutNetworkCalls() = runBlocking {
        val pager = LibraryCataloguePager(load = { _, _, _ -> error("No request expected") })
        val result = pager.page(emptyList(), "", 1)
        assertTrue(result.items.isEmpty())
        assertFalse(result.hasNextPage)
    }

    @Test fun expiredOrChangedLibrarySessionsCannotResumeAnUnrelatedPage() = runBlocking {
        var time = 0L
        val pager = LibraryCataloguePager(load = { connection, _, _ ->
            CataloguePage((1L..40L).map { card(it, connection.libraryId) }, false)
        }, now = { time })
        pager.page(listOf(first, second), "", 1)
        assertThrows(ApiFailure::class.java) { runBlocking { pager.page(listOf(second), "", 2) } }
        time = 300_000
        assertThrows(ApiFailure::class.java) { runBlocking { pager.page(listOf(first, second), "", 2) } }
        Unit
    }
}
