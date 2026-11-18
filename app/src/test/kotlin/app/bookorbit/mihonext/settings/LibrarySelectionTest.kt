package app.bookorbit.mihonext.settings

import app.bookorbit.mihonext.LibraryFilter
import app.bookorbit.mihonext.api.ApiFailure
import app.bookorbit.mihonext.api.BookOrbitApi
import app.bookorbit.mihonext.model.Identity
import app.bookorbit.mihonext.model.Library
import kotlinx.serialization.encodeToString
import org.junit.Assert.*
import org.junit.Test

class LibrarySelectionTest {
    private val libraries = listOf(Library(7, "Comics"), Library(8, "Manga"))
    private val draft = Draft("https://books.example/orbit/", "bok_" + "a".repeat(49))
    private fun configured() = SlotState(draft).applyDiscovery(Discovery(draft, 1, libraries, 1))

    @Test fun verificationAppliesEveryLibraryAndFilterDefaultsToAll() {
        val settings = configured()
        val filter = LibraryFilter(settings.connections())
        assertEquals("라이브러리", filter.name)
        assertEquals("전체 라이브러리", filter.values.first())
        assertEquals(listOf(7L, 8L), filter.connections(settings).map { it.libraryId })
        assertTrue(settings.libraryBindings.all { it.apiKey.isEmpty() })
        assertEquals(2, settings.connections().size)
    }

    @Test fun filteringDoesNotChangeSavedWorksChaptersImagesOrTheAppliedRegistry() {
        val settings = configured()
        val original = settings.connection(7)
        val work = Identity.work(original.binding, 7, "series", 4)
        val chapter = Identity.chapter(work, 11, 22)
        val cover = original.url("books/11/cover")
        val filter = LibraryFilter(settings.connections()).apply { state = 2 }
        assertEquals(8L, filter.connections(settings).single().libraryId)
        assertEquals(original, settings.connectionForWork(work))
        assertEquals(original, settings.connectionForWork(chapter))
        assertEquals(original, settings.connectionForImage(cover))
        filter.state = 0
        assertEquals(listOf(7L, 8L), filter.connections(settings).map { it.libraryId })
    }

    @Test fun filterSnapshotSurvivesRenameReorderingAndKeyRotation() {
        val first = configured()
        val filter = LibraryFilter(first.connections()).apply { state = 2 }
        val selected = filter.connections(first).single()
        val rotated = draft.copy(apiKey = "bok_" + "b".repeat(49))
        val renamed = libraries.reversed().map { it.copy(name = "Renamed ${it.id}") }
        val next = first.copy(draft = rotated).applyDiscovery(Discovery(rotated, 1, renamed, 2))
        val resolved = filter.connections(next).single()
        assertEquals(8L, resolved.libraryId)
        assertEquals(selected.binding, resolved.binding)
        assertEquals(rotated.apiKey, resolved.apiKey)
        assertEquals("Renamed 8", resolved.libraryName)
        assertEquals(listOf(7L, 8L), LibraryFilter(next.connections()).connections(next).map { it.libraryId })
    }

    @Test fun staleSingleAndAllFiltersWorksAndImagesRejectAnotherServerOrAccount() {
        val first = configured()
        val filter = LibraryFilter(first.connections()).apply { state = 2 }
        val all = LibraryFilter(first.connections())
        val original = filter.connections(first).single()
        val work = Identity.work(original.binding, 8, "book", 11)
        for ((changed, account) in listOf(draft.copy(server = "https://other.example/") to 1L, draft to 2L)) {
            val next = first.copy(draft = changed).applyDiscovery(Discovery(changed, account, libraries, 2))
            assertThrows(ApiFailure::class.java) { filter.connections(next) }
            assertThrows(ApiFailure::class.java) { all.connections(next) }
            assertThrows(ApiFailure::class.java) { next.connectionForWork(work) }
            assertThrows(ApiFailure::class.java) { next.connectionForImage(original.url("books/11/cover")) }
        }
    }

    @Test fun removedLibrariesFailExplicitlyAndRestoreTheirBindingWhenRediscovered() {
        val first = configured()
        val filter = LibraryFilter(first.connections()).apply { state = 2 }
        val original = filter.connections(first).single()
        val reduced = first.applyDiscovery(Discovery(draft, 1, libraries.take(1), 2))
        assertThrows(ApiFailure::class.java) { filter.connections(reduced) }
        assertThrows(ApiFailure::class.java) { reduced.connectionForImage(original.url("books/11/cover")) }
        val restored = reduced.applyDiscovery(Discovery(draft, 1, libraries, 3))
        assertEquals(original.binding, filter.connections(restored).single().binding)
    }

    @Test fun legacySingleLibrarySettingsRetainHistoryWhenAllLibrariesAreApplied() {
        val first = configured()
        // Simulate the legacy default being library 8, while the first discovered library is 7.
        val legacy = first.copy(active = first.connection(8), libraryBindings = emptyList())
        val encoded = BookOrbitApi.json.encodeToString(legacy)
        assertFalse(encoded.contains("libraryBindings"))
        val decoded = BookOrbitApi.json.decodeFromString<SlotState>(encoded)
        val original = decoded.connection(8)
        val work = Identity.work(original.binding, 8, "series", 4)
        val migrated = decoded.applyDiscovery(Discovery(draft, 1, libraries, 2))
        assertEquals(original, migrated.connectionForWork(work))
        assertEquals(setOf(7L, 8L), migrated.connections().map { it.libraryId }.toSet())
    }

    @Test fun draftEditsCannotChangeTheAppliedServerOrImageCredentials() {
        val first = configured()
        val next = first.copy(draft = Draft("https://pending.example/", "pending"), discovery = null)
        assertEquals(first.connections(), next.connections())
        val original = first.connection(8)
        assertEquals(original, next.connectionForImage(original.url("cbz/files/22/pages/0")))
    }

    @Test fun currentLibraryIdentitiesAreNotEvictedByThePreviousConnectionLimit() {
        val many = (1L..20L).map { Library(it, "Library $it") }
        val first = SlotState(draft).applyDiscovery(Discovery(draft, 1, many, 1))
        val next = first.applyDiscovery(Discovery(draft, 1, many.reversed(), 2))
        assertEquals(20, next.connections().size)
        for (connection in first.connections()) {
            assertEquals(connection.binding, next.connection(connection.libraryId).binding)
        }
    }

    @Test fun zeroAccessibleLibrariesIsAVerifiedEmptyRegistryAndDoesNotRestoreRemovedAccess() {
        val first = configured()
        val empty = first.applyDiscovery(Discovery(draft, 1, emptyList(), 2))
        assertNotNull(empty.active)
        assertTrue(empty.connections().isEmpty())
        assertTrue(LibraryFilter(empty.connections()).connections(empty).isEmpty())
        assertThrows(ApiFailure::class.java) { empty.connection(7) }
        val restored = empty.applyDiscovery(Discovery(draft, 1, libraries, 3))
        assertEquals(first.connection(7).binding, restored.connection(7).binding)
    }

    @Test fun staleAndInvalidFilterSelectionsFailWithoutChoosingAnotherLibrary() {
        val stale = LibraryFilter(emptyList())
        assertEquals(1, stale.values.size)
        assertThrows(ApiFailure::class.java) { stale.connections(configured()) }
        val invalid = LibraryFilter(configured().connections()).apply { state = 99 }
        assertThrows(ApiFailure::class.java) { invalid.connections(configured()) }
    }
}
