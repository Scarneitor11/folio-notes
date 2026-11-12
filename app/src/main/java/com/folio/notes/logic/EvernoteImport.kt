package com.folio.notes.logic

import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Reads Evernote's .enex export. ENEX is XML with one <note> element per note;
 * attachments are ignored, while title, tags, timestamps and readable ENML body
 * text are preserved. Parsing is local and bounded by the same limit as Keep.
 */
object EvernoteImport {

    private const val MAX_NOTES = 4_000
    private val CHECKED_TODO = Regex(
        """(?is)<en-todo[^>]*checked\s*=\s*["']true["'][^>]*/?>""",
    )
    private val TODO = Regex("""(?is)<en-todo[^>]*/?>""")
    private val MEDIA = Regex("""(?is)<en-media[^>]*/?>""")


    fun parse(name: String, bytes: ByteArray): KeepImportResult {
        if (bytes.isEmpty() || bytes.size > KeepImport.MAX_IMPORT_BYTES) {
            return KeepImportResult(emptyList(), 0, 1)
        }
        return runCatching {
            val factory = DocumentBuilderFactory.newInstance().apply {
                isNamespaceAware = false
                isExpandEntityReferences = false
                // ENEX includes a DOCTYPE. Do not fetch its external DTD.
                runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
                runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
                runCatching {
                    setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
                }
                runCatching { isXIncludeAware = false }
            }
            val document = factory.newDocumentBuilder().parse(ByteArrayInputStream(bytes))
            val nodes = document.getElementsByTagName("note")
            val notes = mutableListOf<KeepNote>()
            for (index in 0 until nodes.length) {
                if (notes.size >= MAX_NOTES) break
                val note = nodes.item(index) as? Element ?: continue
                val title = note.childText("title").trim()
                val body = enmlToMarkdown(note.childText("content"))
                if (title.isBlank() && body.isBlank()) continue
                val created = parseDate(note.childText("created"))
                val updated = parseDate(note.childText("updated"))
                notes += KeepNote(
                    sourceId = "evernote:$title:${created}",
                    title = title,
                    body = body,
                    labels = note.childValues("tag"),
                    createdAt = created,
                    updatedAt = updated,
                )
            }
            KeepImportResult(notes, 1, 0)
        }.getOrElse { KeepImportResult(emptyList(), 0, 1) }
    }

    private fun enmlToMarkdown(content: String): String {
        if (content.isBlank()) return ""
        var text = content
        text = CHECKED_TODO.replace(text, "- [x] ")
        text = TODO.replace(text, "- [ ] ")
        text = MEDIA.replace(text, "")
        return KeepImport.htmlToMarkdown(text).replace(Regex("[ \\t]{2,}"), " ")
    }

    private fun parseDate(raw: String): Long {
        val value = raw.trim()
        if (value.isEmpty()) return 0L
        // SimpleDateFormat is mutable and not thread-safe; keep it local to the parse.
        val format = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply {
            isLenient = false
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val parsed = runCatching { format.parse(value) }.getOrNull() ?: return 0L
        return parsed.time
    }

    private fun Element.childText(tag: String): String {
        val children = childNodes
        for (index in 0 until children.length) {
            val node = children.item(index)
            if (node is Element && node.tagName.equals(tag, ignoreCase = true)) {
                return node.textContent.orEmpty()
            }
        }
        return ""
    }

    private fun Element.childValues(tag: String): List<String> {
        val out = mutableListOf<String>()
        val children = childNodes
        for (index in 0 until children.length) {
            val node = children.item(index)
            if (node !is Element || !node.tagName.equals(tag, ignoreCase = true)) continue
            val clean = node.textContent.orEmpty().trim().removePrefix("#").trim()
            if (clean.isNotEmpty() && out.none { it.equals(clean, ignoreCase = true) }) {
                out += clean
            }
        }
        return out.take(100)
    }
}
