package app.bookorbit.mihonext.repository

import app.bookorbit.mihonext.api.ApiFailure
import app.bookorbit.mihonext.api.BookOrbitApi
import app.bookorbit.mihonext.api.FailureKind
import app.bookorbit.mihonext.model.*
import app.bookorbit.mihonext.settings.Connection
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class BookOrbitRepository(private val connection: Connection, private val api: BookOrbitApi) {
    private val pager = FilteredPager({ query, page ->
        val response = api.catalogue(page, query)
        response.copy(items = response.items.map { representative(it) ?: it })
    })
    private val workLock = Mutex()
    private data class Entry(val time: Long, val work: Work)
    private val works = LinkedHashMap<String, Entry>(16, 0.75f, true)

    suspend fun catalogue(page: Int, query: String) = pager.page(query.trim(), page)

    private suspend fun representative(card: BookCard): BookCard? {
        if (card.readableFiles().isNotEmpty() || card.seriesId == null) return card
        // The server format filter also matches attachments. A collapsed representative with
        // no content archive must not hide a later readable volume of the same series.
        repeat(100) { page ->
            val books = api.seriesBooks(card.seriesId, page)
            if (books.seriesInfo?.id != card.seriesId || books.items.any { it.seriesId != card.seriesId }) throw ApiFailure.protocol()
            books.items.firstOrNull { it.readableFiles().isNotEmpty() }?.let { return it }
            if ((page + 1L) * books.size >= books.total) return null
        }
        throw ApiFailure(FailureKind.PROTOCOL, "시리즈 형식 확인 한도에 도달했습니다. 서버 파일 상태를 확인하세요.")
    }

    suspend fun work(url: String, fresh: Boolean = false): Work = workLock.withLock {
        val identity = Identity.parse(url, connection.binding, connection.libraryId)
        val now = System.currentTimeMillis()
        works.entries.removeAll { now - it.value.time > 120_000 }
        if (!fresh) works[url]?.let { return@withLock it.work }
        val result = if (identity.kind == "book") {
            val detail = api.book(identity.id)
            Work(detail.title ?: "Book ${detail.id}", listOf(detail.card()), detail.description)
        } else {
            val books = ArrayList<BookCard>()
            val ids = HashSet<Long>()
            var name: String? = null
            var rawPage = 0
            while (true) {
                val response = api.seriesBooks(identity.id, rawPage)
                val info = response.seriesInfo ?: throw ApiFailure.protocol()
                if (info.id != identity.id) throw ApiFailure.protocol()
                name = info.name
                for (book in response.items) {
                    if (book.seriesId != identity.id) throw ApiFailure.protocol()
                    if (!ids.add(book.id)) throw ApiFailure(FailureKind.PROTOCOL, "시리즈 내용이 조회 중 변경되었습니다. 다시 불러오세요.")
                    if (book.readableFiles().isNotEmpty()) books += book
                }
                rawPage++
                if (rawPage.toLong() * response.size >= response.total) break
                if (rawPage >= 100) throw ApiFailure(FailureKind.PROTOCOL, "한 시리즈의 도서가 10,000권을 초과합니다.")
            }
            Work(name, books)
        }
        val readable = result.copy(books = result.books.filter { it.readableFiles().isNotEmpty() })
        if (readable.books.isEmpty()) throw ApiFailure(FailureKind.NOT_FOUND, "현재 라이브러리에 읽을 수 있는 만화 파일이 없습니다.")
        if (readable.books.sumOf { it.readableFiles().size } > 10_000) throw ApiFailure.protocol()
        works[url] = Entry(now, readable)
        // Bound by files as well as entries, to avoid retaining many large series.
        while (works.size > 16 || works.values.sumOf { it.work.books.sumOf { b -> b.files.size } } > 10_000) {
            works.remove(works.keys.first())
        }
        readable
    }

    fun chapters(work: Work): List<ComicChapter> = work.books.flatMap { book ->
        book.readableFiles().sortedBy { it.id }.map { ComicChapter(book, it) }
    }

    suspend fun pages(chapterUrl: String): List<String> {
        val identity = Identity.parse(chapterUrl, connection.binding, connection.libraryId)
        val bookId = identity.bookId ?: throw ApiFailure.protocol()
        val fileId = identity.fileId ?: throw ApiFailure.protocol()
        val workUrl = Identity.work(connection.binding, connection.libraryId, identity.kind, identity.id)
        val chapter = chapters(work(workUrl, fresh = true)).firstOrNull { it.book.id == bookId && it.file.id == fileId }
            ?: throw ApiFailure(FailureKind.NOT_FOUND, "만화 파일이 삭제되었거나 더 이상 읽을 수 없습니다.")
        return (0 until api.pageCount(chapter.file.id)).map {
            connection.url("cbz/files/$fileId/pages/$it").toString()
        }
    }
}
