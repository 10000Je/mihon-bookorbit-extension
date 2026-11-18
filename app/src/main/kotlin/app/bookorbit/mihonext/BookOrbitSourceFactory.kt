package app.bookorbit.mihonext

import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceFactory
import app.bookorbit.mihonext.model.Identity

/** Fixed source set: Mihon retains factory results until extension/process reload. */
class BookOrbitSourceFactory : SourceFactory {
    override fun createSources(): List<Source> = (1..Identity.SOURCE_COUNT).map { slot ->
        BookOrbitSource(slot)
    }
}
