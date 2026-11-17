package app.bookorbit.mihonext.model

/** Pure format policy. Actual API DTO mapping belongs in the repository layer. */
object ComicFormat {
    private val readable = setOf("cbz", "cbr", "cb7")

    fun normalize(value: String?): String? = value
        ?.trim()
        ?.lowercase()
        ?.removePrefix(".")
        ?.takeIf { it.isNotBlank() }

    fun isReadable(value: String?): Boolean = normalize(value) in readable

    fun hasReadableFile(formats: Iterable<String?>): Boolean = formats.any(::isReadable)
}
