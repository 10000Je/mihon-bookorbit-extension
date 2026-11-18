package app.bookorbit.mihonext

import app.bookorbit.mihonext.api.ApiFailure
import app.bookorbit.mihonext.settings.Connection
import app.bookorbit.mihonext.settings.SlotState
import eu.kanade.tachiyomi.source.model.Filter

/** Keep IDs and bindings with this snapshot so renames/reordering cannot change its meaning. */
class LibraryFilter(connections: List<Connection>) : Filter.Select<String>(
    "라이브러리",
    (listOf("전체 라이브러리") + connections.map { "${it.libraryName} (#${it.libraryId})" }).toTypedArray(),
) {
    private val choices = connections.map { it.libraryId to it.binding }

    fun connections(settings: SlotState): List<Connection> {
        if (state == 0) {
            val current = settings.connections()
            if (current.map { it.libraryId to it.binding }.toSet() != choices.toSet()) throw ApiFailure.configuration()
            return current
        }
        val (id, binding) = choices.getOrNull(state - 1) ?: throw ApiFailure.configuration()
        return listOf(settings.connection(id, binding))
    }
}
