package app.bookorbit.mihonext.repository

import app.bookorbit.mihonext.api.ApiFailure
import app.bookorbit.mihonext.api.FailureKind
import app.bookorbit.mihonext.model.BookCard
import app.bookorbit.mihonext.model.BooksPage
import app.bookorbit.mihonext.model.CataloguePage
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Server filters normally do all the work; look-ahead also handles attachment-only cards. */
class FilteredPager(
    private val load: suspend (String, Int) -> BooksPage,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private data class Cursor(val rawPage: Int = 0, val ended: Boolean = false, val pending: List<BookCard> = emptyList())
    private data class Cached(val result: CataloguePage, val next: Cursor)
    private class Session(val created: Long) {
        val pages = LinkedHashMap<Int, Cached>()
    }
    private val lock = Mutex()
    private val sessions = LinkedHashMap<String, Session>(4, 0.75f, true)

    suspend fun page(query: String, page: Int): CataloguePage = lock.withLock {
        require(page >= 1)
        val timestamp = now()
        sessions.entries.removeAll { timestamp - it.value.created >= 300_000 }
        if (page == 1) sessions.remove(query)
        val session = sessions[query] ?: if (page == 1) Session(timestamp).also {
            sessions[query] = it
            while (sessions.size > 4) sessions.remove(sessions.keys.first())
        } else throw expired()
        session.pages[page]?.let { return@withLock it.result }
        var cursor = if (page == 1) Cursor() else session.pages[page - 1]?.next ?: throw expired()
        val seen = session.pages.values.flatMap { it.result.items }.map { it.identity }.toMutableSet()

        suspend fun scan(start: Cursor): Cursor {
            var current = start
            repeat(128) {
                if (current.ended) return current.copy(pending = emptyList())
                val raw = load(query, current.rawPage)
                if (raw.page != current.rawPage || raw.size !in 1..200 || raw.items.size > raw.size || raw.total < 0 ||
                    (raw.items.isEmpty() && raw.page.toLong() * raw.size < raw.total)) throw ApiFailure.protocol()
                val next = current.rawPage + 1
                val visible = raw.items.filter { it.readableFiles().isNotEmpty() && seen.add(it.identity) }
                current = Cursor(next, next.toLong() * raw.size >= raw.total, visible)
                if (visible.isNotEmpty()) return current
            }
            throw ApiFailure(FailureKind.PROTOCOL, "목록 보충 읽기 한도에 도달했습니다. 서버 형식 필터와 파일 상태를 확인하세요.")
        }

        if (cursor.pending.isEmpty()) cursor = scan(cursor)
        val visible = cursor.pending
        seen.addAll(visible.map { it.identity })
        val next = scan(cursor.copy(pending = emptyList()))
        val result = CataloguePage(visible, next.pending.isNotEmpty())
        // Commit only after all requests succeed, so retries never skip a partially read page.
        session.pages[page] = Cached(result, next)
        while (session.pages.size > 32) session.pages.remove(session.pages.keys.first())
        result
    }

    private fun expired() = ApiFailure(FailureKind.CONFIGURATION, "목록 캐시가 만료되었습니다. 첫 페이지에서 다시 탐색하세요.")
}
