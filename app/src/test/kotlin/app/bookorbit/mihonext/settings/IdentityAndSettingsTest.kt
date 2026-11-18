package app.bookorbit.mihonext.settings

import app.bookorbit.mihonext.api.ApiFailure
import app.bookorbit.mihonext.model.Identity
import app.bookorbit.mihonext.model.Library
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class IdentityAndSettingsTest {
    private val key = "bok_" + "a".repeat(49)
    private fun discovered(server: String = "https://books.example/", account: Long = 1, lib: Long = 1, name: String = "A", state: SlotState = SlotState()): SlotState {
        val draft = Draft(server, key)
        return state.copy(draft = draft, discovery = Discovery(draft, account, listOf(Library(lib, name)), 1))
    }

    @Test fun libraryRenameReorderAndKeyRotationPreserveSourceAndWorkIdentity() {
        val first = discovered().bind(Library(1, "A"))
        val rotated = Draft(first.draft.server, "bok_" + "b".repeat(49))
        val renamed = first.copy(draft = rotated, discovery = Discovery(rotated, 1, listOf(Library(2, "B"), Library(1, "New")), 2)).bind(Library(1, "New"))
        assertEquals(first.active!!.binding, renamed.active!!.binding)
        assertEquals("New", renamed.active!!.libraryName)
        assertEquals(641_300_700_001L, Identity.sourceId(1))
        assertEquals(listOf(641_300_700_001L, 641_300_700_002L, 641_300_700_003L),
            (1..Identity.SOURCE_COUNT).map(Identity::sourceId))
        assertThrows(IllegalArgumentException::class.java) { Identity.sourceId(4) }
    }

    @Test fun serverAccountAndLibraryChangesCannotAttachOldChaptersToNewData() {
        val first = discovered().bind(Library(1, "A"))
        val original = first.active!!
        val url = Identity.work(original.binding, 1, "series", 19)
        val chapter = Identity.chapter(url, 11, 22)
        for (changed in listOf(discovered(server = "https://other.example/", state = first),
            discovered(account = 2, state = first), discovered(lib = 2, state = first))) {
            val next = changed.bind(changed.discovery!!.libraries.single()).active!!
            assertNotEquals(original.binding, next.binding)
            assertThrows(IOException::class.java) { Identity.parse(chapter, next.binding, next.libraryId) }
        }
        assertEquals(22L, Identity.parse(chapter, original.binding, 1).fileId)
    }

    @Test fun restoringPreviousConnectionRestoresHistoryButDoesNotRetainOldKeys() {
        val first = discovered().bind(Library(1, "A"))
        val other = discovered(lib = 2, state = first).bind(Library(2, "B"))
        assertTrue(other.previous.all { it.apiKey.isEmpty() })
        val restored = discovered(state = other).bind(Library(1, "A"))
        assertEquals(first.active!!.binding, restored.active!!.binding)
    }

    @Test fun staleDiscoveryAndUnknownLibrariesAreRejected() {
        val state = discovered()
        assertThrows(ApiFailure::class.java) { state.bind(Library(99, "absent")) }
        assertThrows(ApiFailure::class.java) { state.copy(draft = Draft("https://changed.example/", key)).bind(Library(1, "A")) }
    }

    @Test fun credentialsAndQueryUrlsAreRejectedAndSubpathIsPreserved() {
        for (url in listOf("http://books.example", "https://user:pass@books.example", "https://books.example/?token=secret", "https://books.example/#token", "not a url")) {
            assertThrows(ApiFailure::class.java) { ServerUrl.parse(url) }
        }
        val root = ServerUrl.parse("https://books.example/orbit")
        assertEquals("https://books.example/orbit/", root.toString())
        val connection = Connection(root.toString(), key, 1, 1, "A", "a".repeat(32))
        assertEquals("/orbit/api/v1/libraries", connection.url("libraries").encodedPath)
        assertFalse(connection.toString().contains(key)); assertFalse(Draft(root.toString(), key).toString().contains(key))
        assertThrows(IOException::class.java) { Identity.parse("/bo/v1/${"a".repeat(32)}/1/book/999999999999999999999999", "a".repeat(32), 1) }
    }
}
