package app.bookorbit.mihonext.settings

import app.bookorbit.mihonext.api.ApiFailure
import app.bookorbit.mihonext.model.Library
import app.bookorbit.mihonext.model.Identity
import kotlinx.serialization.Serializable
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.util.UUID

@Serializable data class Draft(val server: String = "", val apiKey: String = "") {
    override fun toString() = "Draft([redacted])"
}
@Serializable data class Discovery(val draft: Draft, val principalId: Long, val libraries: List<Library>, val fetchedAt: Long)
@Serializable data class Connection(
    val server: String, val apiKey: String, val principalId: Long,
    val libraryId: Long, val libraryName: String, val binding: String,
) {
    fun url(path: String): HttpUrl = ServerUrl.parse(server).newBuilder()
        .addPathSegments("api/v1/$path").fragment("bo=$binding").build()

    override fun toString() = "Connection([redacted])"
}
@Serializable data class SlotState(
    val draft: Draft = Draft(), val discovery: Discovery? = null, val active: Connection? = null,
    val previous: List<Connection> = emptyList(),
    // Registry entries omit credentials; connections resolve them from active.
    val libraryBindings: List<Connection> = emptyList(),
) {
    /** Apply the verified server/account and every accessible library; there is no default library. */
    fun applyDiscovery(found: Discovery): SlotState {
        if (found.draft != draft) throw ApiFailure.configuration()
        if (found.principalId <= 0 || found.libraries.size > 2000 || found.libraries.any { it.id <= 0 } ||
            found.libraries.distinctBy { it.id }.size != found.libraries.size) throw ApiFailure.protocol()
        val known = listOfNotNull(active) + libraryBindings + previous
        val knownByScope = known.groupBy { Triple(it.server, it.principalId, it.libraryId) }
        val bindings = found.libraries.sortedBy { it.id }.map { entry ->
            val existing = knownByScope[Triple(draft.server, found.principalId, entry.id)]?.firstOrNull()
            Connection(draft.server, "", found.principalId, entry.id, entry.name,
                existing?.binding ?: UUID.randomUUID().toString().replace("-", ""))
        }
        // Preserve the serialized field as a credential anchor, never a catalogue selection.
        // ID 0 represents a verified account with no accessible libraries.
        val next = (bindings.firstOrNull() ?: Connection(draft.server, "", found.principalId, 0, "", "0".repeat(32)))
            .copy(apiKey = draft.apiKey)
        val currentBindings = bindings.map { it.binding }.toSet()
        return copy(discovery = found, active = next, libraryBindings = bindings, previous = known
            .filter { it.libraryId > 0 && it.binding !in currentBindings }
            .distinctBy { it.binding }.take(12).map { it.copy(apiKey = "") })
    }

    fun connections(): List<Connection> {
        val current = active ?: return emptyList()
        return libraryBindings.ifEmpty { listOf(current) }.filter {
            it.libraryId > 0 && it.server == current.server && it.principalId == current.principalId
        }.distinctBy { it.libraryId }.sortedBy { it.libraryId }.map { it.copy(apiKey = current.apiKey) }
    }

    fun connection(libraryId: Long? = null, binding: String? = null): Connection = connections()
        .firstOrNull { (libraryId == null || it.libraryId == libraryId) && (binding == null || it.binding == binding) }
        ?: throw ApiFailure.configuration()

    fun connectionForWork(url: String): Connection {
        val parts = url.split('/')
        val current = connection(parts.getOrNull(4)?.toLongOrNull() ?: throw ApiFailure.configuration(),
            parts.getOrNull(3) ?: throw ApiFailure.configuration())
        Identity.parse(url, current.binding, current.libraryId)
        return current
    }

    fun connectionForImage(url: HttpUrl): Connection {
        val binding = url.fragment?.takeIf { it.startsWith("bo=") }?.removePrefix("bo=")
            ?: throw ApiFailure.configuration()
        return connection(binding = binding)
    }
}

object ServerUrl {
    fun parse(raw: String): HttpUrl {
        val url = raw.trim().toHttpUrlOrNull() ?: throw ApiFailure.configuration()
        if (url.scheme != "https" || url.username.isNotEmpty() || url.password.isNotEmpty() ||
            url.query != null || url.fragment != null) {
            throw ApiFailure(app.bookorbit.mihonext.api.FailureKind.CONFIGURATION,
                "서버 URL은 자격 증명·쿼리 없는 HTTPS 주소여야 합니다. 하위 경로는 사용할 수 있습니다.")
        }
        return url.newBuilder().encodedPath(url.encodedPath.trimEnd('/') + "/").build()
    }
}
