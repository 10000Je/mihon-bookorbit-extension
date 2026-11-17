package app.bookorbit.mihonext.model

import kotlinx.serialization.Serializable

@Serializable data class Library(val id: Long, val name: String)
@Serializable data class Principal(val id: Long, val authenticationMethod: String)
@Serializable data class ComicFile(val id: Long, val format: String? = null, val role: String = "")

@Serializable
data class BookCard(
    val id: Long,
    val libraryId: Long? = null,
    val title: String? = null,
    val status: String,
    val authors: List<String> = emptyList(),
    val seriesId: Long? = null,
    val seriesName: String? = null,
    val seriesIndex: String? = null,
    val files: List<ComicFile>,
    val hasCover: Boolean = false,
    val genres: List<String> = emptyList(),
    val addedAt: String? = null,
) {
    fun readableFiles(): List<ComicFile> = if (status == "present") files.filter {
        it.id > 0 && it.role in setOf("primary", "content") && ComicFormat.isReadable(it.format)
    } else emptyList()

    val identity: String get() = seriesId?.let { "series/$it" } ?: "book/$id"
}

@Serializable data class SeriesInfo(val id: Long, val name: String, val authors: List<String> = emptyList())
@Serializable data class BooksPage(
    val items: List<BookCard>, val total: Long, val page: Int, val size: Int,
    val seriesInfo: SeriesInfo? = null,
)
@Serializable data class Author(val name: String)
@Serializable data class BookDetail(
    val id: Long, val libraryId: Long, val title: String? = null, val status: String,
    val files: List<ComicFile>, val authors: List<Author> = emptyList(),
    val description: String? = null, val genres: List<String> = emptyList(),
    val seriesIndex: String? = null, val addedAt: String? = null,
) {
    fun card() = BookCard(id, libraryId, title, status, authors.map { it.name },
        seriesIndex = seriesIndex, files = files, genres = genres, addedAt = addedAt)
}
@Serializable data class PageCount(val pageCount: Int)

data class CataloguePage(val items: List<BookCard>, val hasNextPage: Boolean)
data class ComicChapter(val book: BookCard, val file: ComicFile)
data class Work(val title: String, val books: List<BookCard>, val description: String? = null)
