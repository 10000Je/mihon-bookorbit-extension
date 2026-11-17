package app.bookorbit.mihonext

import androidx.preference.PreferenceScreen
import app.bookorbit.mihonext.api.ApiFailure
import app.bookorbit.mihonext.api.BookOrbitApi
import app.bookorbit.mihonext.api.BookOrbitHttp
import app.bookorbit.mihonext.model.BookCard
import app.bookorbit.mihonext.model.Identity
import app.bookorbit.mihonext.repository.BookOrbitRepository
import app.bookorbit.mihonext.settings.Connection
import app.bookorbit.mihonext.settings.SettingsScreen
import app.bookorbit.mihonext.settings.SettingsStore
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.source.model.*
import eu.kanade.tachiyomi.source.online.HttpSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Headers
import okhttp3.OkHttpClient
import java.time.Instant

class BookOrbitSource(private val slot: Int) : HttpSource(), ConfigurableSource {
    override val id: Long = Identity.sourceId(slot)
    override val name: String get() = runCatching { SettingsStore.read(slot).active?.libraryName }.getOrNull()
        ?.let { "BookOrbit · $it (슬롯 $slot)" } ?: "BookOrbit · 슬롯 $slot"
    override val lang = "all"
    override val supportsLatest = false
    override val baseUrl: String get() = active().server.trimEnd('/')
    override val client: OkHttpClient by lazy { BookOrbitHttp.client { active() } }
    override fun headersBuilder(): Headers.Builder = Headers.Builder().add("Accept", "image/*, application/json")
    override fun getFilterList() = FilterList()

    private data class BoundRepository(val connection: Connection, val repository: BookOrbitRepository)
    @Volatile private var bound: BoundRepository? = null
    private fun active() = SettingsStore.read(slot).active ?: throw ApiFailure.configuration()

    @Synchronized private fun repository(): BoundRepository {
        val config = active()
        return bound?.takeIf { it.connection == config } ?: BoundRepository(
            config, BookOrbitRepository(config, BookOrbitApi(config, client)),
        ).also { bound = it }
    }

    override suspend fun getPopularManga(page: Int): MangasPage = catalogue(page, "")
    override suspend fun getSearchManga(page: Int, query: String, filters: FilterList): MangasPage = catalogue(page, query)
    override suspend fun getLatestUpdates(page: Int): MangasPage = throw UnsupportedOperationException("이 소스는 최신 목록을 지원하지 않습니다. 인기를 사용하세요.")

    private suspend fun catalogue(page: Int, query: String): MangasPage = withContext(Dispatchers.IO) {
        val current = repository()
        val result = current.repository.catalogue(page, query)
        ensureCurrent(current.connection)
        MangasPage(result.items.map { manga(it, current.connection) }, result.hasNextPage)
    }

    override suspend fun getMangaUpdate(
        manga: SManga, chapters: List<SChapter>, fetchDetails: Boolean, fetchChapters: Boolean,
    ): SMangaUpdate = withContext(Dispatchers.IO) {
        if (!fetchDetails && !fetchChapters) return@withContext SMangaUpdate(manga, chapters)
        val current = repository()
        val work = current.repository.work(manga.url, fresh = true)
        val updated = if (fetchDetails) manga(work.books.first(), current.connection).apply {
            url = manga.url
            title = work.title
            author = work.books.flatMap { it.authors }.distinct().joinToString(", ")
            genre = work.books.flatMap { it.genres }.distinct().joinToString(", ")
            description = work.description ?: "${work.books.size}권 · CBZ / CBR / CB7 만화 파일"
            thumbnail_url = work.books.firstOrNull { it.hasCover }?.let {
                current.connection.url("books/${it.id}/cover").toString()
            } ?: manga.thumbnail_url
            initialized = true
        } else manga
        val nextChapters = if (fetchChapters) current.repository.chapters(work).map { comic ->
            SChapter.create().apply {
                url = Identity.chapter(manga.url, comic.book.id, comic.file.id)
                name = listOfNotNull(comic.book.seriesIndex?.let { "$it 권" }, comic.book.title,
                    comic.file.format?.uppercase()).joinToString(" · ")
                chapter_number = comic.book.seriesIndex?.toFloatOrNull()?.takeIf { it.isFinite() && it >= 0 } ?: -1f
                date_upload = comic.book.addedAt?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() } ?: 0
                scanlator = comic.file.format?.uppercase()
            }
        }.asReversed() else chapters
        ensureCurrent(current.connection)
        SMangaUpdate(updated, nextChapters)
    }

    override suspend fun getPageList(chapter: SChapter): List<Page> = withContext(Dispatchers.IO) {
        val current = repository()
        val pages = current.repository.pages(chapter.url)
        ensureCurrent(current.connection)
        pages.mapIndexed { index, url -> Page(index, imageUrl = url) }
    }

    override fun getMangaUrl(manga: SManga): String {
        val config = active()
        val identity = Identity.parse(manga.url, config.binding, config.libraryId)
        return config.server.trimEnd('/') + if (identity.kind == "book") "/book/${identity.id}" else "/series/${identity.id}"
    }
    override fun getChapterUrl(chapter: SChapter): String {
        val config = active()
        val identity = Identity.parse(chapter.url, config.binding, config.libraryId)
        return config.server.trimEnd('/') + "/book/${identity.bookId ?: identity.id}"
    }

    override fun setupPreferenceScreen(screen: PreferenceScreen) = SettingsScreen(slot) { bound = null }.setup(screen)

    private fun manga(book: BookCard, config: Connection): SManga = SManga.create().apply {
        url = Identity.work(config.binding, config.libraryId, if (book.seriesId != null) "series" else "book", book.seriesId ?: book.id)
        title = book.seriesName?.takeIf { book.seriesId != null && it.isNotBlank() } ?: book.title ?: "Book ${book.id}"
        author = book.authors.joinToString(", ")
        genre = book.genres.joinToString(", ")
        thumbnail_url = if (book.hasCover) config.url("books/${book.id}/cover").toString() else null
        status = SManga.UNKNOWN
    }

    private fun ensureCurrent(config: Connection) {
        if (active() != config) throw ApiFailure(app.bookorbit.mihonext.api.FailureKind.CONFIGURATION,
            "조회 중 연결 설정이 변경되었습니다. 다시 불러오세요.")
    }
}
