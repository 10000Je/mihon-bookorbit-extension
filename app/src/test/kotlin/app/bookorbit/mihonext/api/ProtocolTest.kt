package app.bookorbit.mihonext.api

import app.bookorbit.mihonext.model.Identity
import app.bookorbit.mihonext.repository.BookOrbitRepository
import app.bookorbit.mihonext.settings.Connection
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

/** Synthetic responses reflecting documented upstream DTOs, not a claim of live-server validation. */
class ProtocolTest {
    private lateinit var server: MockWebServer
    private lateinit var connection: Connection
    private lateinit var client: okhttp3.OkHttpClient
    private lateinit var api: BookOrbitApi
    private val key = "bok_" + "s".repeat(49)

    @Before fun setup() {
        val cert = HeldCertificate.Builder().commonName("localhost").addSubjectAlternativeName("localhost").build()
        val serverTls = HandshakeCertificates.Builder().heldCertificate(cert).build()
        val clientTls = HandshakeCertificates.Builder().addTrustedCertificate(cert.certificate).build()
        server = MockWebServer().apply { useHttps(serverTls.sslSocketFactory()); start() }
        connection = Connection(server.url("/orbit/").toString(), key, 1, 7, "Comics", "a".repeat(32))
        client = BookOrbitHttp.client { connection }.newBuilder()
            .sslSocketFactory(clientTls.sslSocketFactory(), clientTls.trustManager).build()
        api = BookOrbitApi(connection, client)
    }
    @After fun tearDown() { server.close() }
    private fun respond(body: String, code: Int = 200) = server.enqueue(MockResponse.Builder().code(code).body(body).build())
    private fun card(id: Int, files: String, series: Int? = 4) = """{"id":$id,"libraryId":7,"status":"present","title":"Book $id","seriesId":$series,"seriesName":"Series","seriesIndex":"$id","files":$files,"authors":["Author"],"hasCover":true} """
    private val cbz get() = """[{"id":22,"format":"CBZ","role":"primary"},{"id":23,"format":"EPUB","role":"content"},{"id":24,"format":"PDF","role":"content"}]"""
    private fun books(items: String, total: Int, page: Int, size: Int = 100) = """{"items":[$items],"total":$total,"page":$page,"size":$size,"seriesInfo":{"id":4,"name":"Renamed","authors":["Author"]}}"""

    @Test fun authLibrariesAndSearchUseVerifiedPathsAndReadSafeBody() = runBlocking {
        respond("""{"id":1,"authenticationMethod":"api_key","username":"ignored"}""")
        respond("""[{"id":7,"name":"Comics","displayOrder":9}]""")
        respond(books(card(11, cbz), 1, 0, 40))
        assertEquals(1L, api.principal().id); assertEquals("Comics", api.libraries().single().name)
        val found = api.catalogue(0, "# query & 한글").items.single()
        assertEquals(listOf(22L), found.readableFiles().map { it.id })
        val me = server.takeRequest(); assertEquals("/orbit/api/v1/auth/me", me.target)
        assertEquals("Bearer $key", me.headers["Authorization"])
        server.takeRequest()
        val request = server.takeRequest()
        assertEquals("POST", request.method); assertEquals("/orbit/api/v1/libraries/7/books", request.target)
        val body = BookOrbitApi.json.parseToJsonElement(request.body!!.utf8()).jsonObject
        assertEquals("# query & 한글", body["q"]!!.jsonPrimitive.content)
        assertEquals(true, body["collapseSeries"]!!.jsonPrimitive.boolean)
        assertEquals(false, body["collapseLinked"]!!.jsonPrimitive.boolean)
        assertEquals(listOf("cbz", "cbr", "cb7"), body["filter"]!!.jsonObject["rules"]!!.jsonArray[0].jsonObject["value"]!!.jsonArray.map { it.jsonPrimitive.content })
        assertFalse(request.target.contains(key)); assertFalse(request.target.contains("bo="))
    }

    @Test fun authenticatedCoversAndPagesUseTheSameClientWithoutCookiesOrQuerySecrets() {
        for (path in listOf("books/11/cover", "cbz/files/22/pages/0")) {
            respond("image-bytes")
            client.newCall(Request.Builder().url(connection.url(path)).header("Cookie", "session=old").build()).execute().use {
                assertEquals("image-bytes", it.body.string())
            }
            val sent = server.takeRequest()
            assertEquals("Bearer $key", sent.headers["Authorization"])
            assertNull(sent.headers["Cookie"]); assertNull(sent.headers["X-Api-Key"])
            assertFalse(sent.target.contains(key)); assertFalse(sent.target.contains("#"))
        }
    }

    @Test fun remappedAndForeignImageUrlsAreBlockedBeforeAnyCredentialIsSent() {
        val stale = connection.url("books/11/cover")
        connection = connection.copy(binding = "b".repeat(32))
        val urls = listOf(stale, server.url("/third-party/image"), server.url("/orbit/api/v1/books/11/cover"))
        for (url in urls) {
            assertThrows(ApiFailure::class.java) { client.newCall(Request.Builder().url(url).build()).execute() }
        }
        assertEquals(0, server.requestCount)
    }

    @Test fun errorsAreDistinctSanitizedAndInvalidCredentialsAreNotRetried() = runBlocking {
        for ((code, kind) in listOf(401 to FailureKind.AUTHENTICATION, 403 to FailureKind.PERMISSION,
            404 to FailureKind.NOT_FOUND, 422 to FailureKind.PROTOCOL, 500 to FailureKind.SERVER)) {
            respond("""{"message":"$key https://private.example/"}""", code)
            val error = assertThrows(ApiFailure::class.java) { runBlocking { api.principal() } }
            assertEquals(kind, error.kind); assertFalse(error.toString().contains(key))
            assertFalse(error.toString().contains("private.example")); assertNull(error.cause)
        }
        assertEquals(5, server.requestCount)
    }

    @Test fun bounded429RetriesAndRedirectsCannotExposeCredentials() = runBlocking {
        repeat(3) { server.enqueue(MockResponse.Builder().code(429).addHeader("Retry-After", "0").build()) }
        assertEquals(FailureKind.RATE_LIMIT, assertThrows(ApiFailure::class.java) { runBlocking { api.principal() } }.kind)
        assertEquals(3, server.requestCount)
        server.enqueue(MockResponse.Builder().code(302).addHeader("Location", "https://foreign.example/").build())
        assertThrows(ApiFailure::class.java) { runBlocking { api.principal() } }
        assertEquals(4, server.requestCount)
        assertNull(BookOrbitHttp.retryDelay(429, "600", 0))
        assertNull(BookOrbitHttp.retryDelay(503, null, 2))
        assertNull(BookOrbitHttp.retryDelay(401, null, 0))
        assertEquals(1000L, BookOrbitHttp.retryDelay(503, "1", 0))
    }

    @Test fun cancellationStopsAnInFlightRequest() = runBlocking {
        server.enqueue(MockResponse.Builder().body("""{"id":1,"authenticationMethod":"api_key"}""")
            .headersDelay(5, TimeUnit.SECONDS).build())
        val job = launch(Dispatchers.IO) { api.principal() }
        withContext(Dispatchers.IO) { assertNotNull(server.takeRequest(2, TimeUnit.SECONDS)) }
        withTimeout(2000) { job.cancelAndJoin() }
        assertTrue(job.isCancelled)
    }

    @Test fun cancellationAlsoStopsReadingASlowResponseBody() = runBlocking {
        server.enqueue(MockResponse.Builder().body("""{"id":1,"authenticationMethod":"api_key"}""")
            .bodyDelay(5, TimeUnit.SECONDS).build())
        val job = launch(Dispatchers.IO) { api.principal() }
        withContext(Dispatchers.IO) { assertNotNull(server.takeRequest(2, TimeUnit.SECONDS)) }
        delay(50)
        withTimeout(2000) { job.cancelAndJoin() }
        assertTrue(job.isCancelled)
    }

    @Test fun wrongSchemaAndCrossLibraryResponsesFailInsteadOfBecomingEmptyResults() = runBlocking {
        respond("""{"items":[],"total":10,"page":0,"size":40}""")
        assertThrows(ApiFailure::class.java) { runBlocking { api.catalogue(0, "") } }
        respond("""{"id":11,"libraryId":99,"status":"present","files":$cbz}""")
        assertEquals(FailureKind.PERMISSION, assertThrows(ApiFailure::class.java) { runBlocking { api.book(11) } }.kind)
        respond("<html>private proxy error</html>")
        assertEquals(FailureKind.PROTOCOL, assertThrows(ApiFailure::class.java) { runBlocking { api.principal() } }.kind)
    }

    @Test fun mixedSeriesCreatesOnlyReadableChaptersAndZeroBasedPageUrls() = runBlocking {
        val repo = BookOrbitRepository(connection, api)
        val url = Identity.work(connection.binding, 7, "series", 4)
        respond(books(card(11, cbz), 1, 0))
        val work = repo.work(url)
        assertEquals("Renamed", work.title)
        val comic = repo.chapters(work).single()
        assertEquals(22L, comic.file.id)
        respond(books(card(11, cbz), 1, 0)); respond("""{"pageCount":3}""")
        val pages = repo.pages(Identity.chapter(url, 11, 22))
        assertEquals((0..2).map { "/orbit/api/v1/cbz/files/22/pages/$it" }, pages.map { it.toHttpUrl().encodedPath })
        val seriesRequest = server.takeRequest()
        assertTrue(seriesRequest.target.contains("libraryId=7")); assertTrue(seriesRequest.target.contains("page=0"))
    }

    @Test fun seriesBooksArePaginatedPastOneHundredEpubVolumesAndIncludeCbrCb7() = runBlocking {
        val repo = BookOrbitRepository(connection, api)
        val epub = """[{"id":1,"format":"epub","role":"primary"}]"""
        respond(books((1..100).joinToString(",") { card(it, epub) }, 102, 0))
        respond(books(card(101, """[{"id":101,"format":"cbr","role":"content"}]""") + "," + card(102, """[{"id":102,"format":"cb7","role":"primary"}]"""), 102, 1))
        val work = repo.work(Identity.work(connection.binding, 7, "series", 4))
        assertEquals(listOf("cbr", "cb7"), repo.chapters(work).map { it.file.format })
        assertEquals(2, server.requestCount)
    }

    @Test fun deletedOrUnopenableFilesProduceExplicitErrors() = runBlocking {
        val repo = BookOrbitRepository(connection, api)
        val url = Identity.work(connection.binding, 7, "book", 11)
        respond("""{"id":11,"libraryId":7,"status":"missing","files":$cbz}""")
        assertEquals(FailureKind.NOT_FOUND, assertThrows(ApiFailure::class.java) { runBlocking { repo.work(url) } }.kind)
        respond("""{"pageCount":0}""")
        assertEquals(FailureKind.PROTOCOL, assertThrows(ApiFailure::class.java) { runBlocking { api.pageCount(22) } }.kind)
    }
}
