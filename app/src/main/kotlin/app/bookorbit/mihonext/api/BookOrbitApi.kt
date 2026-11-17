package app.bookorbit.mihonext.api

import app.bookorbit.mihonext.model.*
import app.bookorbit.mihonext.settings.Connection
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.*
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.io.ByteArrayOutputStream

class BookOrbitApi(private val connection: Connection, private val client: OkHttpClient) {
    suspend fun principal(): Principal = read(connection.url("auth/me"))
    suspend fun libraries(): List<Library> = read<List<Library>>(connection.url("libraries")).also { list ->
        if (list.size > 2000 || list.any { it.id <= 0 } || list.map { it.id }.distinct().size != list.size) throw ApiFailure.protocol()
    }

    suspend fun catalogue(page: Int, query: String): BooksPage = read<BooksPage>(
        connection.url("libraries/${connection.libraryId}/books"), catalogueQuery(page, query).toString(),
    ).checked(page, CATALOGUE_SIZE)

    suspend fun seriesBooks(seriesId: Long, page: Int): BooksPage = read<BooksPage>(
        connection.url("series/$seriesId/books").newBuilder()
            .addQueryParameter("libraryId", connection.libraryId.toString())
            .addQueryParameter("page", page.toString()).addQueryParameter("size", "100")
            .addQueryParameter("sort", "seriesIndex").addQueryParameter("order", "asc").build(),
    ).checked(page, 100)

    suspend fun book(bookId: Long): BookDetail = read<BookDetail>(connection.url("books/$bookId")).also {
        if (it.id != bookId || it.libraryId != connection.libraryId) throw ApiFailure(FailureKind.PERMISSION, "작품이 선택한 라이브러리에 속하지 않습니다.")
    }

    suspend fun pageCount(fileId: Long): Int = read<PageCount>(connection.url("cbz/files/$fileId/pages")).pageCount.also {
        if (it !in 1..20_000) throw ApiFailure(FailureKind.PROTOCOL, "만화 페이지 수가 비어 있거나 지원 범위를 벗어났습니다.")
    }

    private fun BooksPage.checked(expectedPage: Int, expectedSize: Int): BooksPage {
        if (page != expectedPage || size != expectedSize || total < 0 || items.size > size ||
            (items.isEmpty() && page.toLong() * size < total) || items.any {
                it.id <= 0 || (it.libraryId != null && it.libraryId != connection.libraryId)
            }) throw ApiFailure.protocol()
        return this
    }

    private suspend inline fun <reified T> read(url: HttpUrl, body: String? = null): T {
        val request = Request.Builder().url(url).header("Accept", "application/json").apply {
            if (body != null) post(body.toRequestBody("application/json; charset=utf-8".toMediaType()))
        }.build()
        return client.newCall(request).awaitParsed { response ->
            if (!response.isSuccessful) throw ApiFailure.http(response.code)
            val responseBody = response.body
            if (responseBody.contentLength() > MAX_JSON_BYTES) throw ApiFailure.protocol()
            val bytes = run {
                val output = ByteArrayOutputStream()
                val input = responseBody.byteStream()
                val buffer = ByteArray(8192)
                while (output.size() <= MAX_JSON_BYTES) {
                    val count = input.read(buffer, 0, minOf(buffer.size, MAX_JSON_BYTES + 1 - output.size()))
                    if (count == -1) break
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
            if (bytes.size > MAX_JSON_BYTES) throw ApiFailure.protocol()
            try { json.decodeFromString<T>(bytes.toString(Charsets.UTF_8)) } catch (_: SerializationException) {
                throw ApiFailure.protocol()
            } catch (_: IllegalArgumentException) { throw ApiFailure.protocol() }
        }
    }

    companion object {
        const val CATALOGUE_SIZE = 40
        private const val MAX_JSON_BYTES = 8 * 1024 * 1024
        val json = Json { ignoreUnknownKeys = true }

        fun catalogueQuery(page: Int, query: String): JsonObject {
            require(page >= 0)
            if (query.length > 200) throw ApiFailure(FailureKind.CONFIGURATION, "검색어는 200자 이하로 입력하세요.")
            return buildJsonObject {
                put("collapseSeries", true)
                put("collapseLinked", false)
                put("q", query.trim())
                putJsonObject("pagination") { put("page", page); put("size", CATALOGUE_SIZE) }
                putJsonArray("sort") { addJsonObject { put("field", "title"); put("dir", "asc") } }
                putJsonObject("filter") {
                    put("type", "group"); put("join", "AND")
                    putJsonArray("rules") {
                        addJsonObject {
                            put("type", "rule"); put("field", "format"); put("operator", "includesAny")
                            putJsonArray("value") { listOf("cbz", "cbr", "cb7").forEach { add(it) } }
                        }
                        addJsonObject { put("type", "rule"); put("field", "fileAvailability"); put("operator", "isPresent") }
                    }
                }
            }
        }
    }
}
