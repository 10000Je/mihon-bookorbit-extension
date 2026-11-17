package app.bookorbit.mihonext.api

import app.bookorbit.mihonext.model.Identity
import app.bookorbit.mihonext.repository.BookOrbitRepository
import app.bookorbit.mihonext.settings.Connection
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import okhttp3.Request
import okhttp3.tls.HandshakeCertificates
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

/** Opt-in, read-only integration test. Configuration contains secrets and must remain outside Git. */
class LiveServerTest {
    @Test fun realServerLibraryIsolationEpubExclusionAndArchiveImages() = runBlocking {
        val path = System.getenv("BOOKORBIT_LIVE_CONFIG")
        assumeTrue("No isolated live-server config supplied", !path.isNullOrBlank())
        val config = BookOrbitApi.json.parseToJsonElement(File(path!!).readText()).jsonObject
        val cert = CertificateFactory.getInstance("X.509").generateCertificate(config["caCertificate"]!!.jsonPrimitive.content.byteInputStream()) as X509Certificate
        val tls = HandshakeCertificates.Builder().addTrustedCertificate(cert).build()
        val libraryIds = config["libraryIds"]!!.jsonArray.map { it.jsonPrimitive.long }
        val formats = mutableSetOf<String>()
        val workIds = mutableSetOf<String>()
        for (libraryId in libraryIds) {
            val connection = Connection(config["server"]!!.jsonPrimitive.content,
                config["apiKey"]!!.jsonPrimitive.content, config["principalId"]!!.jsonPrimitive.long,
                libraryId, "Integration", "f".repeat(32))
            val client = BookOrbitHttp.client { connection }.newBuilder()
                .sslSocketFactory(tls.sslSocketFactory(), tls.trustManager).build()
            val api = BookOrbitApi(connection, client)
            assertEquals(connection.principalId, api.principal().id)
            assertEquals(libraryIds.toSet(), api.libraries().map { it.id }.toSet())
            val repository = BookOrbitRepository(connection, api)
            val page = repository.catalogue(1, "")
            assertFalse(page.hasNextPage)
            assertEquals(if (libraryId == libraryIds.first()) 5 else 1, page.items.size)
            assertTrue(page.items.none { it.title?.contains("Epub Only") == true })
            for (card in page.items) {
                assertEquals(libraryId, card.libraryId)
                val url = Identity.work(connection.binding, libraryId, if (card.seriesId == null) "book" else "series", card.seriesId ?: card.id)
                assertTrue(workIds.add(url))
                val work = repository.work(url)
                val chapters = repository.chapters(work)
                assertEquals(1, chapters.size)
                for (chapter in chapters) {
                    val format = chapter.file.format!!.lowercase()
                    formats += format
                    val pages = repository.pages(Identity.chapter(url, chapter.book.id, chapter.file.id))
                    assertEquals(3, pages.size)
                    for (image in pages) {
                        client.newCall(Request.Builder().url(image).build()).awaitResponse().use { response ->
                            assertEquals("image/png", response.header("Content-Type"))
                            assertTrue(response.body.bytes().take(8).toByteArray().contentEquals(byteArrayOf(-119,80,78,71,13,10,26,10)))
                        }
                    }
                }
                if (card.hasCover) {
                    client.newCall(Request.Builder().url(connection.url("books/${card.id}/cover")).build()).awaitResponse().use {
                        assertTrue(it.header("Content-Type")!!.startsWith("image/")); assertTrue(it.body.bytes().isNotEmpty())
                    }
                }
            }
            val search = repository.catalogue(1, "Epub Only")
            assertTrue(search.items.isEmpty()); assertFalse(search.hasNextPage)
            val matching = repository.catalogue(1, if (libraryId == libraryIds.first()) "Mixed" else "Library")
            assertEquals(1, matching.items.size)
        }
        assertEquals(setOf("cbz", "cbr", "cb7"), formats)
        println("Verified real BookOrbit: 3 isolated libraries, 7 works, 3 archive formats, 21 authenticated page images, covers, search and EPUB exclusion")
    }
}
