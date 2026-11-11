package com.folio.notes.logic

import com.folio.notes.data.Note

/**
 * Renders a note as portable Markdown for the Android share sheet. Kept free of
 * Android types so the exact shared text can be unit-tested.
 */
object NoteExport {

    fun markdown(note: Note, untitled: String = "Untitled"): String {
        val title = note.title.trim().ifBlank { untitled }
        val body = note.body.trimEnd()
        val tags = note.tags.map { it.trim().removePrefix("#") }.filter { it.isNotEmpty() }
        return buildString {
            append("# ").append(title).append("\n\n")
            if (body.isNotEmpty()) append(body).append("\n\n")
            if (tags.isNotEmpty()) append(tags.joinToString(" ") { "#$it" })
        }.trimEnd() + "\n"
    }
}
