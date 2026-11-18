package app.bookorbit.mihonext.model

import java.io.IOException

object Identity {
    const val SOURCE_COUNT = 3
    fun sourceName(slot: Int): String {
        require(slot in 1..SOURCE_COUNT)
        return "BookOrbit " + listOf("I", "II", "III")[slot - 1]
    }
    const val BASE_SLOT_SOURCE_ID = 641_300_700_000L
    fun sourceId(slot: Int): Long {
        require(slot in 1..SOURCE_COUNT)
        return BASE_SLOT_SOURCE_ID + slot
    }

    fun work(binding: String, libraryId: Long, kind: String, id: Long): String {
        require(kind == "series" || kind == "book")
        require(id > 0 && libraryId > 0)
        require(binding.matches(Regex("[a-f0-9]{32}")))
        return "/bo/v1/$binding/$libraryId/$kind/$id"
    }

    fun chapter(work: String, bookId: Long, fileId: Long): String {
        require(bookId > 0 && fileId > 0)
        return "$work/book/$bookId/file/$fileId"
    }

    fun parse(url: String, binding: String, libraryId: Long): WorkId {
        val match = Regex("^/bo/v1/([a-f0-9]{32})/([1-9][0-9]*)/(series|book)/([1-9][0-9]*)(?:/book/([1-9][0-9]*)/file/([1-9][0-9]*))?$").matchEntire(url)
            ?: throw IOException("저장된 작품 식별자가 올바르지 않습니다.")
        if (match.groupValues[1] != binding || match.groupValues[2].toLongOrNull() != libraryId) {
            throw IOException("이 작품은 이전 연결에 속합니다. 원래 서버·계정 연결을 복원하거나 새 소스로 이관하세요.")
        }
        return WorkId(match.groupValues[3], match.groupValues[4].toLongOrNull()
            ?: throw IOException("작품 식별자 범위 오류"),
            match.groupValues[5].takeIf { it.isNotEmpty() }?.toLongOrNull(),
            match.groupValues[6].takeIf { it.isNotEmpty() }?.toLongOrNull())
    }
}

data class WorkId(val kind: String, val id: Long, val bookId: Long? = null, val fileId: Long? = null)
