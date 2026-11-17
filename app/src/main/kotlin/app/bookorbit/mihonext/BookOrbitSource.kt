package app.bookorbit.mihonext

import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.SMangaUpdate

/**
 * Thin host adapter skeleton. Every network/content function intentionally fails loudly:
 * do not interpret an empty page as a working BookOrbit client.
 */
class BookOrbitSource(private val slot: Int) : Source {
    init { require(slot in 1..6) }

    override val id: Long = BASE_SLOT_SOURCE_ID + slot
    override val name: String = "BookOrbit · 슬롯 $slot"
    override val lang: String = "all"
    override val supportsLatest: Boolean = false

    override suspend fun getPopularManga(page: Int): MangasPage = notImplemented()

    override suspend fun getLatestUpdates(page: Int): MangasPage =
        MangasPage(emptyList(), false) // Host should hide this entry because supportsLatest=false.

    override suspend fun getSearchManga(page: Int, query: String, filters: FilterList): MangasPage =
        notImplemented()

    override suspend fun getMangaUpdate(
        manga: SManga,
        chapters: List<SChapter>,
        fetchDetails: Boolean,
        fetchChapters: Boolean,
    ): SMangaUpdate = notImplemented()

    override suspend fun getPageList(chapter: SChapter): List<Page> = notImplemented()

    private fun <T> notImplemented(): T =
        throw UnsupportedOperationException("BookOrbit API is not connected. See CODEX_PROMPT.md")

    private companion object {
        // Stable placeholder slot source IDs; replace ONLY with explicit migration policy.
        const val BASE_SLOT_SOURCE_ID = 641_300_700_000L
    }
}
