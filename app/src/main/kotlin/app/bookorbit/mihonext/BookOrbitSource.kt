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
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Headers
import okhttp3.OkHttpClient
import java.time.Instant

class BookOrbitSource(private val slot: Int) : HttpSource(), ConfigurableSource {
    override val id: Long = Identity.sourceId(slot)
    override val name: String = "BookOrbit · 슬롯 $slot"
    override val lang = "all"
    override val supportsLatest = false
    override val baseUrl: String get() = runCatching { SettingsStore.read(slot).active?.server?.trimEnd('/') }
        .getOrNull() ?: "https://bookorbit.invalid"
    override val client: OkHttpClient by lazy {
        BookOrbitHttp.scopedClient { SettingsStore.read(slot).connectionForImage(it) }
    }
    override fun headersBuilder(): Headers.Builder = Headers.Builder().add("Accept", "image/*, application/json")
    override fun getFilterList() = FilterList(LibraryFilter(SettingsStore.read(slot).connections()))

    private data class BoundRepository(val connection: Connection, val repository: BookOrbitRepository)
    private val repositories = LinkedHashMap<String, BoundRepository>(4, 0.75f, true)
    @Synchronized private fun repository(config: Connection): BoundRepository {
        return repositories[config.binding]?.takeIf { it.connection == config } ?: BoundRepository(
            config, BookOrbitRepository(config, BookOrbitApi(config, client)),
        ).also {
            repositories[config.binding] = it
            while (repositories.size > 4) repositories.remove(repositories.keys.first())
        }
    }

    override suspend fun getPopularManga(page: Int): MangasPage = catalogue(page, "")
    override suspend fun getSearchManga(page: Int, query: String, filters: FilterList): MangasPage =
        catalogue(page, query, filters.filterIsInstance<LibraryFilter>().firstOrNull())
    override suspend fun getLatestUpdates(page: Int): MangasPage = throw UnsupportedOperationException("이 소스는 최신 목록을 지원하지 않습니다. 인기를 사용하세요.")

    private suspend fun catalogue(page: Int, query: String, filter: LibraryFilter? = null): MangasPage = withContext(Dispatchers.IO) {
        val settings = SettingsStore.read(slot)
        val current = repository(filter?.connection(settings) ?: settings.connection())
        val result = withTimeoutOrNull(90_000) { current.repository.catalogue(page, query) } ?: operationTimeout()
        ensureCurrent(current.connection)
        MangasPage(result.items.map { manga(it, current.connection) }, result.hasNextPage)
    }

    override suspend fun getMangaUpdate(
        manga: SManga, chapters: List<SChapter>, fetchDetails: Boolean, fetchChapters: Boolean,
    ): SMangaUpdate = withContext(Dispatchers.IO) {
        if (!fetchDetails && !fetchChapters) return@withContext SMangaUpdate(manga, chapters)
        val current = repository(SettingsStore.read(slot).connectionForWork(manga.url))
        val work = withTimeoutOrNull(120_000) { current.repository.work(manga.url, fresh = true) } ?: operationTimeout()
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
        val current = repository(SettingsStore.read(slot).connectionForWork(chapter.url))
        val pages = withTimeoutOrNull(120_000) { current.repository.pages(chapter.url) } ?: operationTimeout()
        ensureCurrent(current.connection)
        pages.mapIndexed { index, url -> Page(index, imageUrl = url) }
    }

    override fun getMangaUrl(manga: SManga): String {
        val config = SettingsStore.read(slot).connectionForWork(manga.url)
        val identity = Identity.parse(manga.url, config.binding, config.libraryId)
        return config.server.trimEnd('/') + if (identity.kind == "book") "/book/${identity.id}" else "/series/${identity.id}"
    }
    override fun getChapterUrl(chapter: SChapter): String {
        val config = SettingsStore.read(slot).connectionForWork(chapter.url)
        val identity = Identity.parse(chapter.url, config.binding, config.libraryId)
        return config.server.trimEnd('/') + "/book/${identity.bookId ?: identity.id}"
    }

    override fun setupPreferenceScreen(screen: PreferenceScreen) = SettingsScreen(slot) { clearRepositories() }.setup(screen)

    @Synchronized private fun clearRepositories() = repositories.clear()

    private fun manga(book: BookCard, config: Connection): SManga = SManga.create().apply {
        url = Identity.work(config.binding, config.libraryId, if (book.seriesId != null) "series" else "book", book.seriesId ?: book.id)
        title = book.seriesName?.takeIf { book.seriesId != null && it.isNotBlank() } ?: book.title ?: "Book ${book.id}"
        author = book.authors.joinToString(", ")
        genre = book.genres.joinToString(", ")
        thumbnail_url = if (book.hasCover) config.url("books/${book.id}/cover").toString() else null
        status = SManga.UNKNOWN
    }

    private fun ensureCurrent(config: Connection) {
        if (SettingsStore.read(slot).connection(config.libraryId, config.binding) != config) throw ApiFailure(app.bookorbit.mihonext.api.FailureKind.CONFIGURATION,
            "조회 중 연결 설정이 변경되었습니다. 다시 불러오세요.")
    }

    private fun operationTimeout(): Nothing = throw ApiFailure(app.bookorbit.mihonext.api.FailureKind.TIMEOUT,
        "조회 시간이 초과되었습니다. 서버 상태와 라이브러리 크기를 확인하고 다시 시도하세요.")
}
