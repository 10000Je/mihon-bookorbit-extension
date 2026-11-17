package app.bookorbit.mihonext.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ComicFormatTest {
    @Test fun acceptsComicArchiveFormats() {
        listOf("cbz", ".CBR", " CB7 ").forEach { assertTrue(ComicFormat.isReadable(it)) }
    }

    @Test fun rejectsEbooksAndUnknownFormats() {
        listOf("epub", "pdf", "azw3", "mobi", "mp3", "", null).forEach {
            assertFalse(ComicFormat.isReadable(it))
        }
    }

    @Test fun ignoresEpubOnlyButAcceptsMixedSeries() {
        assertFalse(ComicFormat.hasReadableFile(listOf("EPUB", "epub")))
        assertTrue(ComicFormat.hasReadableFile(listOf("epub", "CBZ")))
    }
}
