package app.bookorbit.mihonext
import android.app.Application
import app.bookorbit.mihonext.model.Library
import app.bookorbit.mihonext.api.BookOrbitApi
import eu.kanade.tachiyomi.source.*
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File

class CachedRegistryProbeFactory : SourceFactory {
    override fun createSources(): List<Source> {
        val registry = File(Injekt.get<Application>().noBackupFilesDir, "bookorbit-cached-registry-probe.json")
        val libraries = if (registry.exists()) BookOrbitApi.json.decodeFromString<List<Library>>(registry.readText()) else emptyList()
        val entries = libraries.ifEmpty { listOf(Library(0, "Guide")) }
        return entries.map { lib -> object : CatalogueSource by BookOrbitSource(1) {
            override val id = 9_105_002_000_000L + lib.id
            override val name = "BookOrbit Probe · ${lib.name}"
        } }
    }
}
