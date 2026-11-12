package com.folio.notes.logic

import java.util.Locale

/** Chooses the right portable-notes parser without making the picker format-specific. */
object MigrationImport {

    fun parse(name: String, mimeType: String, bytes: ByteArray): KeepImportResult {
        if (bytes.isEmpty()) return KeepImportResult(emptyList(), 0, 1)
        val lowerName = name.lowercase(Locale.ROOT)
        val mime = mimeType.lowercase(Locale.ROOT)
        val prefix = bytes.take(512).toByteArray().toString(Charsets.UTF_8)
        return when {
            lowerName.endsWith(".enex") ||
                mime.contains("enex") ||
                prefix.contains("<en-export", ignoreCase = true) ->
                EvernoteImport.parse(name, bytes)

            lowerName.endsWith(".md") ||
                lowerName.endsWith(".markdown") ||
                mime == "text/markdown" ->
                MarkdownImport.parse(name, bytes.toString(Charsets.UTF_8))

            else -> KeepImport.parse(name, bytes)
        }
    }
}
