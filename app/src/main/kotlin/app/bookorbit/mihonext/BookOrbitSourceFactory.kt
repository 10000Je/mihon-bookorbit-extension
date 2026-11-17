package app.bookorbit.mihonext

import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceFactory

/**
 * STARTER ONLY: Six stable placeholders until BookOrbit libraries are discoverable.
 * Codex must first test a cached dynamic source list, falling back to slots if necessary.
 */
class BookOrbitSourceFactory : SourceFactory {
    override fun createSources(): List<Source> = (1..6).map { slot ->
        BookOrbitSource(slot)
    }
}
