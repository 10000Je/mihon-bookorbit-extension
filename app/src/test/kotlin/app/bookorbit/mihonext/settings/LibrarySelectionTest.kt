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
    private fun configured() = SlotState(draft, Discovery(draft, 1, libraries, 1)).bind(libraries.first())

    @Test fun filterAndDefaultChangesKeepSavedWorksChaptersAndImagesInTheirOriginalLibrary() {
        val first = configured()
        val original = first.connection()
        val work = Identity.work(original.binding, 7, "series", 4)
        val chapter = Identity.chapter(work, 11, 22)
        val cover = original.url("books/11/cover")
        val filter = LibraryFilter(first.connections()).apply { state = 1 }
        assertEquals("라이브러리", filter.name)
        assertEquals(8L, filter.connection(first).libraryId)
        assertEquals(7L, first.connection().libraryId)

        val next = first.bind(libraries.last())
        assertEquals(8L, next.connection().libraryId)
        assertEquals(original, next.connectionForWork(work))
        assertEquals(original, next.connectionForWork(chapter))
        assertEquals(original, next.connectionForImage(cover))
        assertEquals(filter.connection(first).binding, next.connection().binding)
        assertTrue(next.libraryBindings.all { it.apiKey.isEmpty() })
    }

    @Test fun filterSnapshotSurvivesRenameReorderingDefaultChangeAndKeyRotation() {
        val first = configured()
        val filter = LibraryFilter(first.connections()).apply { state = 1 }
        val selected = filter.connection(first)
        val rotated = draft.copy(apiKey = "bok_" + "b".repeat(49))
        val renamed = libraries.reversed().map { it.copy(name = "Renamed ${it.id}") }
        val next = first.copy(draft = rotated, discovery = Discovery(rotated, 1, renamed, 2)).bind(renamed.first())
        val resolved = filter.connection(next)
        assertEquals(8L, resolved.libraryId)
        assertEquals(selected.binding, resolved.binding)
        assertEquals(rotated.apiKey, resolved.apiKey)
        assertEquals("Renamed 8", resolved.libraryName)
        assertEquals(8L, LibraryFilter(next.connections()).connection(next).libraryId)
    }

    @Test fun staleFiltersWorksAndImagesCannotResolveAgainstAnotherServerOrAccount() {
        val first = configured()
        val filter = LibraryFilter(first.connections()).apply { state = 1 }
        val original = filter.connection(first)
        val work = Identity.work(original.binding, 8, "book", 11)
        for ((changed, account) in listOf(draft.copy(server = "https://other.example/") to 1L, draft to 2L)) {
            val next = first.copy(draft = changed, discovery = Discovery(changed, account, libraries, 2))
                .bind(libraries.first())
            assertThrows(ApiFailure::class.java) { filter.connection(next) }
            assertThrows(ApiFailure::class.java) { next.connectionForWork(work) }
            assertThrows(ApiFailure::class.java) { next.connectionForImage(original.url("books/11/cover")) }
        }
    }

    @Test fun removedLibrariesFailExplicitlyAndRestoreTheirBindingWhenReapplied() {
        val first = configured()
        val filter = LibraryFilter(first.connections()).apply { state = 1 }
        val original = filter.connection(first)
        val reduced = first.copy(discovery = Discovery(draft, 1, libraries.take(1), 2)).bind(libraries.first())
        assertThrows(ApiFailure::class.java) { filter.connection(reduced) }
        assertThrows(ApiFailure::class.java) { reduced.connectionForImage(original.url("books/11/cover")) }
        val restored = reduced.copy(discovery = Discovery(draft, 1, libraries, 3)).bind(libraries.first())
        assertEquals(original.binding, filter.connection(restored).binding)
    }

    @Test fun legacySingleLibrarySettingsKeepTheirIdentityUntilDiscoveryIsApplied() {
        val first = configured()
        val legacy = first.copy(libraryBindings = emptyList())
        // Encode without the new default-valued field, as the old version did.
        val encoded = BookOrbitApi.json.encodeToString(legacy)
        assertFalse(encoded.contains("libraryBindings"))
        val decoded = BookOrbitApi.json.decodeFromString<SlotState>(encoded)
        assertEquals(first.active, decoded.connection())
        assertEquals(1, decoded.connections().size)
        val migrated = decoded.bind(libraries.first())
        assertEquals(first.active, migrated.connection())
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
        val first = SlotState(draft, Discovery(draft, 1, many, 1)).bind(many.first())
        val next = many.fold(first) { state, library -> state.bind(library) }
        assertEquals(20, next.connections().size)
        for (connection in first.connections()) {
            assertEquals(connection.binding, next.connection(connection.libraryId).binding)
        }
    }

    @Test fun unconfiguredAndInvalidFilterSelectionsFailWithoutChoosingAnotherLibrary() {
        val empty = LibraryFilter(emptyList())
        assertEquals(1, empty.values.size)
        assertThrows(ApiFailure::class.java) { empty.connection(configured()) }
        val invalid = LibraryFilter(configured().connections()).apply { state = 99 }
        assertThrows(ApiFailure::class.java) { invalid.connection(configured()) }
    }
}
