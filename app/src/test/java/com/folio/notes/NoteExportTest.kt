package com.folio.notes

import com.folio.notes.data.Note
import com.folio.notes.logic.NoteExport
import org.junit.Assert.assertEquals
import org.junit.Test

class NoteExportTest {

    @Test
    fun writesTitleBodyAndTags() {
        val note = Note(
            id = "1",
            title = "Quarter plan",
            body = "Line one\nLine two",
            tags = listOf("work", "q4"),
        )
        assertEquals(
            "# Quarter plan\n\nLine one\nLine two\n\n#work #q4\n",
            NoteExport.markdown(note),
        )
    }

    @Test
    fun blankTitleUsesTheProvidedFallback() {
        val note = Note(id = "1", body = "Body")
        assertEquals("# Untitled\n\nBody\n", NoteExport.markdown(note))
        assertEquals("# Ohne Titel\n\nBody\n", NoteExport.markdown(note, untitled = "Ohne Titel"))
    }

    @Test
    fun tagsAreNormalisedAndBlankOnesDropped() {
        val note = Note(id = "1", title = "T", tags = listOf("#idea", "  ", "two words"))
        assertEquals("# T\n\n#idea #two words\n", NoteExport.markdown(note))
    }

    @Test
    fun blankNoteStaysCompact() {
        assertEquals("# T\n", NoteExport.markdown(Note(id = "1", title = "T")))
    }
}
