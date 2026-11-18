package app.bookorbit.mihonext.repository

import app.bookorbit.mihonext.api.ApiFailure
import app.bookorbit.mihonext.api.FailureKind
import app.bookorbit.mihonext.model.BookCard
import app.bookorbit.mihonext.model.CataloguePage
import app.bookorbit.mihonext.settings.Connection
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class LibraryBook(val connection: Connection, val book: BookCard) {
    val identity: String get() = "${connection.binding}/${connection.libraryId}/${book.identity}"
}
data class LibraryCataloguePage(val items: List<LibraryBook>, val hasNextPage: Boolean)

/** Concatenate verified per-library catalogues in library-ID order, retaining each book's scope. */
class LibraryCataloguePager(
    private val load: suspend (Connection, String, Int) -> CataloguePage,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private data class Key(val connections: List<Connection>, val query: String)
    private data class Cursor(val library: Int = 0, val page: Int = 1, val pending: List<LibraryBook> = emptyList())
    private data class Cached(val result: LibraryCataloguePage, val next: Cursor)
    private class Session(val created: Long) {
        val pages = LinkedHashMap<Int, Cached>()
    }
    private val lock = Mutex()
    private val sessions = LinkedHashMap<Key, Session>(4, 0.75f, true)

    suspend fun page(connections: List<Connection>, query: String, page: Int): LibraryCataloguePage = lock.withLock {
        require(page >= 1)
        val key = Key(connections.sortedBy { it.libraryId }, query.trim())
        if (key.connections.map { it.libraryId }.distinct().size != key.connections.size) throw ApiFailure.protocol()
        val timestamp = now()
        sessions.entries.removeAll { timestamp - it.value.created >= 300_000 }
        if (page == 1) sessions.remove(key)
        val session = sessions[key] ?: if (page == 1) Session(timestamp).also {
            sessions[key] = it
            while (sessions.size > 4) sessions.remove(sessions.keys.first())
        } else throw expired()
        session.pages[page]?.let { return@withLock it.result }
        var cursor = if (page == 1) Cursor() else session.pages[page - 1]?.next ?: throw expired()
        val seen = session.pages.values.flatMap { it.result.items }.map { it.identity }.toMutableSet()
        var requests = 0

        suspend fun fill() {
            while (cursor.pending.isEmpty() && cursor.library < key.connections.size) {
                if (++requests > 2048) throw ApiFailure(FailureKind.PROTOCOL, "전체 라이브러리 탐색 한도에 도달했습니다. 라이브러리 필터를 사용하세요.")
                val connection = key.connections[cursor.library]
                val result = load(connection, key.query, cursor.page)
                if (result.items.size > 40 || (result.items.isEmpty() && result.hasNextPage)) throw ApiFailure.protocol()
                val visible = result.items.map { book ->
                    if (book.libraryId != null && book.libraryId != connection.libraryId) throw ApiFailure.protocol()
                    LibraryBook(connection, book)
                }.filter { it.book.readableFiles().isNotEmpty() && seen.add(it.identity) }
                cursor = if (result.hasNextPage) Cursor(cursor.library, cursor.page + 1, visible)
                    else Cursor(cursor.library + 1, 1, visible)
            }
        }

        val items = ArrayList<LibraryBook>(40)
        while (items.size < 40) {
            fill()
            if (cursor.pending.isEmpty()) break
            val count = minOf(40 - items.size, cursor.pending.size)
            val taken = cursor.pending.take(count)
            items += taken
            seen.addAll(taken.map { it.identity })
            cursor = cursor.copy(pending = cursor.pending.drop(count))
        }
        // Look ahead so hasNextPage never advertises only empty libraries or unreadable works.
        fill()
        val result = LibraryCataloguePage(items, cursor.pending.isNotEmpty())
        // Keep failure/cancellation transactional; retry starts from the last committed cursor.
        session.pages[page] = Cached(result, cursor)
        while (session.pages.size > 32) session.pages.remove(session.pages.keys.first())
        result
    }

    private fun expired() = ApiFailure(FailureKind.CONFIGURATION, "목록 캐시가 만료되었습니다. 첫 페이지에서 다시 탐색하세요.")
}
