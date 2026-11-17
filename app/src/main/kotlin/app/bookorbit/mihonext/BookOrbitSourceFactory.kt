package app.bookorbit.mihonext

import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceFactory

/** Fixed source set: Mihon retains factory results until extension/process reload. */
class BookOrbitSourceFactory : SourceFactory {
    override fun createSources(): List<Source> = (1..6).map { slot ->
        BookOrbitSource(slot)
    }
}
