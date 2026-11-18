package app.bookorbit.mihonext

import app.bookorbit.mihonext.api.ApiFailure
import app.bookorbit.mihonext.settings.Connection
import app.bookorbit.mihonext.settings.SlotState
import eu.kanade.tachiyomi.source.model.Filter

/** Keep IDs and bindings with this snapshot so renames/reordering cannot change its meaning. */
class LibraryFilter(connections: List<Connection>) : Filter.Select<String>(
    "라이브러리",
    connections.map { "${it.libraryName} (#${it.libraryId})" }.ifEmpty {
        listOf("소스 설정에서 연결 후 라이브러리를 적용하세요")
    }.toTypedArray(),
) {
    private val choices = connections.map { it.libraryId to it.binding }

    fun connection(settings: SlotState): Connection {
        val (id, binding) = choices.getOrNull(state) ?: throw ApiFailure.configuration()
        return settings.connection(id, binding)
    }
}
