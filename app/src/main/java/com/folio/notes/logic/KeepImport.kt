package com.folio.notes.logic

import com.folio.notes.data.Note
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.UUID
import java.util.zip.ZipInputStream

/**
 * One note read from a Google Keep export. [sourceId] is the file stem Keep uses,
 * so the same note can be recognised when an archive is imported twice.
 */
data class KeepNote(
    val sourceId: String,
    val title: String,
    val body: String,
    val labels: List<String> = emptyList(),
    val pinned: Boolean = false,
    val archived: Boolean = false,
    val trashed: Boolean = false,
    val tint: Int = -1,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
)

data class KeepImportResult(
    val notes: List<KeepNote>,
    val filesRead: Int,
    val skipped: Int,
)

/**
 * Reads Google Takeout Keep exports without third-party libraries. JSON is treated
 * as the authoritative copy, because HTML-only exports lose check states and Keep
 * metadata. The HTML/text parser still makes old or unpacked exports usable.
 */
object KeepImport {

    const val MAX_IMPORT_BYTES = 64 * 1024 * 1024
    private const val MAX_ENTRY_BYTES = 8 * 1024 * 1024
    private const val MAX_ENTRIES = 4_000
    private const val MAX_LABELS = 100

    private val SCRIPT_STYLE = Regex(
        "(?is)<(?:script|style)[^>]*>.*?</(?:script|style)>",
    )
    private val BR = Regex("(?is)<br\\s*/?>")
    private val P_OPEN = Regex("(?is)<p(?:\\s[^>]*)?>")
    private val P_CLOSE = Regex("(?is)</p\\s*>")
    private val DIV_OPEN = Regex("(?is)<div(?:\\s[^>]*)?>")
    private val DIV_CLOSE = Regex("(?is)</div\\s*>")
    private val H_OPEN = Regex("(?is)<h([1-6])(?:\\s[^>]*)?>")
    private val H_CLOSE = Regex("(?is)</h[1-6]\\s*>")
    private val LI_OPEN = Regex("(?is)<li(?:\\s[^>]*)?>")
    private val LI_CLOSE = Regex("(?is)</li\\s*>")
    private val BOLD_OPEN = Regex("(?is)<(?:b|strong)(?:\\s[^>]*)?>")
    private val BOLD_CLOSE = Regex("(?is)</(?:b|strong)>")
    private val ITALIC_OPEN = Regex("(?is)<(?:i|em)(?:\\s[^>]*)?>")
    private val ITALIC_CLOSE = Regex("(?is)</(?:i|em)>")
    private val STRIKE_OPEN = Regex("(?is)<(?:s|strike|del)(?:\\s[^>]*)?>")
    private val STRIKE_CLOSE = Regex("(?is)</(?:s|strike|del)>")
    private val LINK = Regex("""(?is)<a[^>]*href=["']([^"']+)["'][^>]*>(.*?)</a>""")
    private val TAG = Regex("(?is)<[^>]+>")
    private val HTML_TITLE = Regex("(?is)<title[^>]*>(.*?)</title>")
    private val BODY = Regex("(?is)<body[^>]*>(.*?)</body>")
    private val ARCHIVED = Regex("""(?is)<(?:div|span)[^>]*\bclass\s*=\s*["'][^"']*\barchived\b""")
    private val TRASHED = Regex("""(?is)<(?:div|span)[^>]*\bclass\s*=\s*["'][^"']*\btrashed\b""")
    private val PINNED = Regex("""(?is)<(?:div|span)[^>]*\bclass\s*=\s*["'][^"']*\bpinned\b""")

    private val NAMED_ENTITIES = mapOf(
        "amp" to "&",
        "lt" to "<",
        "gt" to ">",
        "quot" to "\"",
        "apos" to "'",
        "nbsp" to " ",
        "ndash" to "–",
        "mdash" to "—",
        "hellip" to "…",
        "rsquo" to "’",
        "lsquo" to "‘",
        "ldquo" to "“",
        "rdquo" to "”",
    )

    /**
     * Parses one URI's bytes. The caller is responsible for reading the bytes off
     * the main thread; this object stays free of Android APIs so it can be tested
     * on the JVM.
     */
    fun parse(name: String, bytes: ByteArray): KeepImportResult {
        if (bytes.isEmpty() || bytes.size > MAX_IMPORT_BYTES) {
            return KeepImportResult(emptyList(), 0, 1)
        }
        return runCatching {
            when {
                isZip(name, bytes) -> parseZip(ByteArrayInputStream(bytes))
                isJson(name, bytes) -> one(parseJson(name, decode(bytes)))
                isHtml(name, bytes) -> one(parseHtml(name, decode(bytes)))
                else -> one(parseText(name, decode(bytes)))
            }
        }.getOrElse { KeepImportResult(emptyList(), 0, 1) }
    }

    fun parseJson(name: String, raw: String): KeepNote? {
        val root = runCatching { JSONObject(raw) }.getOrNull() ?: return null
        val title = if (root.isNull("title")) "" else root.optString("title").trim()
        val html = if (root.isNull("textContentHtml")) "" else root.optString("textContentHtml")
        val plain = if (root.isNull("textContent")) "" else root.optString("textContent").trim()
        val textBody = if (html.isNotBlank()) htmlToMarkdown(html) else plain
        val listBody = listBody(root.optJSONArray("listContent"))
        val body = listOf(textBody, listBody)
            .filter { it.isNotBlank() }
            .joinToString("\n\n")
            .trim()
        if (title.isBlank() && body.isBlank()) return null

        return KeepNote(
            sourceId = baseName(name),
            title = title,
            body = body,
            labels = labels(root.opt("labels")),
            pinned = root.optBoolean("isPinned", false),
            archived = root.optBoolean("isArchived", false),
            trashed = root.optBoolean("isTrashed", false),
            tint = colorTint(root.optString("color")),
            createdAt = timestamp(root, "createdTimestampUsec", "createdTimestamp"),
            updatedAt = timestamp(
                root,
                "userEditedTimestampUsec",
                "userEditedTimestamp",
                "editedTimestampUsec",
                "updatedTimestampUsec",
            ),
        )
    }

    fun parseHtml(name: String, raw: String): KeepNote? {
        val clean = SCRIPT_STYLE.replace(raw, "")
        val title = (classBlock(clean, "title")?.let(::htmlToMarkdown).orEmpty())
            .ifBlank { HTML_TITLE.find(clean)?.groupValues?.get(1)?.let(::htmlToMarkdown).orEmpty() }
            .trim()
        val content = classBlock(clean, "content")
            ?: BODY.find(clean)?.groupValues?.get(1)
            ?: clean
        val body = htmlToMarkdown(content)
        if (title.isBlank() && body.isBlank()) return null

        val labelsBlock = classBlock(clean, "labels")?.let(::htmlToMarkdown).orEmpty()
        return KeepNote(
            sourceId = baseName(name),
            title = title,
            body = body,
            labels = labels(labelsBlock),
            pinned = PINNED.containsMatchIn(clean),
            archived = ARCHIVED.containsMatchIn(clean),
            trashed = TRASHED.containsMatchIn(clean),
        )
    }

    fun parseText(name: String, raw: String): KeepNote? {
        val body = raw.trim()
        if (body.isEmpty()) return null
        val title = baseName(name)
            .replace('_', ' ')
            .replace('-', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()
        return KeepNote(sourceId = baseName(name), title = title, body = body)
    }

    /** Bounded stream read shared with the ContentResolver import path. */
    fun readLimited(input: InputStream, maxBytes: Int = MAX_IMPORT_BYTES): ByteArray? {
        if (maxBytes <= 0) return ByteArray(0)
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        var total = 0
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            if (total > maxBytes) return null
            out.write(buffer, 0, read)
        }
        return out.toByteArray()
    }

    fun parseZip(input: InputStream): KeepImportResult {
        val jsonEntries = mutableListOf<Pair<String, String>>()
        val htmlEntries = mutableListOf<Pair<String, String>>()
        val textEntries = mutableListOf<Pair<String, String>>()
        var considered = 0
        var filesRead = 0
        var skipped = 0
        var totalBytes = 0

        try {
            ZipInputStream(input).use { zip ->
                var entries = 0
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (entry.isDirectory) continue
                    if (++entries > MAX_ENTRIES) {
                        skipped++
                        break
                    }
                    val path = entry.name.replace('\\', '/')
                    if (!isNoteFile(path)) continue
                    considered++
                    val data = readLimited(zip, MAX_ENTRY_BYTES)
                    if (data == null) {
                        skipped++
                        continue
                    }
                    totalBytes += data.size
                    if (totalBytes > MAX_IMPORT_BYTES) {
                        skipped++
                        break
                    }
                    filesRead++
                    val lower = path.lowercase()
                    when {
                        lower.endsWith(".json") -> jsonEntries += path to decode(data)
                        lower.endsWith(".html") || lower.endsWith(".htm") ->
                            htmlEntries += path to decode(data)
                        else -> textEntries += path to decode(data)
                    }
                }
            }
        } catch (_: Exception) {
            return KeepImportResult(emptyList(), filesRead, skipped + 1)
        }

        val jsonNotes = jsonEntries.mapNotNull { (path, raw) -> parseJson(path, raw) }
        val notes = if (jsonNotes.isNotEmpty()) {
            jsonNotes
        } else {
            htmlEntries.mapNotNull { (path, raw) -> parseHtml(path, raw) }
                .ifEmpty { textEntries.mapNotNull { (path, raw) -> parseText(path, raw) } }
        }
        return KeepImportResult(
            notes = notes.distinctBy { it.sourceId },
            filesRead = filesRead,
            skipped = (skipped + considered - notes.size).coerceAtLeast(0),
        )
    }

    private fun one(note: KeepNote?): KeepImportResult =
        KeepImportResult(listOfNotNull(note), 1, if (note == null) 1 else 0)

    private fun isZip(name: String, bytes: ByteArray): Boolean =
        name.endsWith(".zip", ignoreCase = true) ||
            (bytes.size >= 4 && bytes[0].toInt() == 0x50 && bytes[1].toInt() == 0x4B)

    private fun isJson(name: String, bytes: ByteArray): Boolean {
        if (name.endsWith(".json", ignoreCase = true)) return true
        val first = bytes.firstOrNull { !it.toInt().toChar().isWhitespace() } ?: return false
        return first.toInt().toChar() == '{'
    }

    private fun isHtml(name: String, bytes: ByteArray): Boolean {
        if (name.endsWith(".html", true) || name.endsWith(".htm", true)) return true
        val start = decode(bytes.take(512).toByteArray()).lowercase()
        return start.contains("<html") || start.contains("<div")
    }

    private fun isNoteFile(path: String): Boolean {
        val file = path.substringAfterLast('/').lowercase()
        if (file == "labels.txt" || file.startsWith("archive_browser.")) return false
        return file.endsWith(".json") || file.endsWith(".html") || file.endsWith(".htm") ||
            file.endsWith(".txt") || file.endsWith(".md")
    }

    private fun listBody(array: JSONArray?): String {
        if (array == null) return ""
        val lines = mutableListOf<String>()
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index)
            val text = item?.optString("text")?.trim()
                ?: array.optString(index).trim()
            if (text.isEmpty()) continue
            val checked = item?.optBoolean("isChecked", false) == true
            lines += "- [${if (checked) "x" else " "}] $text"
        }
        return lines.joinToString("\n")
    }

    private fun labels(raw: Any?): List<String> {
        val out = mutableListOf<String>()
        fun add(value: String?) {
            val clean = value?.trim()?.removePrefix("#")?.trim().orEmpty()
            if (clean.isNotEmpty() && out.none { it.equals(clean, ignoreCase = true) }) {
                out += clean
            }
        }

        when (raw) {
            is JSONArray -> for (index in 0 until raw.length()) {
                when (val item = raw.opt(index)) {
                    is JSONObject -> add(item.optString("name"))
                    is String -> add(item)
                }
            }
            is String -> raw.split(',', '\n', ';').forEach(::add)
        }
        return out.take(MAX_LABELS)
    }

    private fun timestamp(root: JSONObject, vararg keys: String): Long {
        for (key in keys) {
            if (!root.has(key) || root.isNull(key)) continue
            val raw = root.optLong(key, 0L)
            if (raw <= 0L) continue
            return when {
                raw >= 100_000_000_000_000L -> raw / 1_000L
                raw >= 10_000_000_000L -> raw
                else -> raw * 1_000L
            }
        }
        return 0L
    }

    private fun colorTint(raw: String): Int = when (raw.trim().uppercase()) {
        "RED" -> 4
        "ORANGE" -> 0
        "YELLOW" -> 5
        "GREEN" -> 2
        "TEAL" -> 1
        "BLUE" -> 1
        "DARKBLUE" -> 6
        "PURPLE" -> 3
        "PINK" -> 7
        "BROWN" -> 0
        else -> -1
    }

    private fun classBlock(html: String, name: String): String? =
        Regex(
            """(?is)<div[^>]*\bclass\s*=\s*["'][^"']*\b${Regex.escape(name)}\b[^"']*["'][^>]*>(.*?)</div\s*>""",
        ).find(html)?.groupValues?.get(1)

    private fun htmlToMarkdown(raw: String): String {
        var text = raw
        text = SCRIPT_STYLE.replace(text, "")
        text = BR.replace(text, "\n")
        text = P_CLOSE.replace(text, "\n\n")
        text = P_OPEN.replace(text, "")
        text = DIV_CLOSE.replace(text, "\n")
        text = DIV_OPEN.replace(text, "")
        text = H_OPEN.replace(text) { match ->
            "#".repeat(match.groupValues[1].toInt()) + " "
        }
        text = H_CLOSE.replace(text, "\n\n")
        text = LI_OPEN.replace(text) { match ->
            if (match.value.contains("checked", ignoreCase = true)) "- [x] " else "- "
        }
        text = LI_CLOSE.replace(text, "\n")
        text = BOLD_OPEN.replace(text, "**")
        text = BOLD_CLOSE.replace(text, "**")
        text = ITALIC_OPEN.replace(text, "_")
        text = ITALIC_CLOSE.replace(text, "_")
        text = STRIKE_OPEN.replace(text, "~~")
        text = STRIKE_CLOSE.replace(text, "~~")
        text = LINK.replace(text) { match ->
            "[${TAG.replace(match.groupValues[2], "").trim()}](${match.groupValues[1]})"
        }
        text = TAG.replace(text, "")
        text = decodeEntities(text).replace('\u00A0', ' ')
        return text.replace("\r\n", "\n")
            .replace('\r', '\n')
            .lines()
            .joinToString("\n") { it.trimEnd() }
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
    }

    private fun decodeEntities(input: String): String {
        val entity = Regex("&(#x?[0-9a-fA-F]+|[A-Za-z]+);")
        return entity.replace(input) { match ->
            val body = match.groupValues[1]
            val code = when {
                body.startsWith("#x", true) -> body.substring(2).toIntOrNull(16)
                body.startsWith("#") -> body.substring(1).toIntOrNull()
                else -> null
            }
            if (code != null && code in 0..0x10FFFF) {
                String(Character.toChars(code))
            } else {
                NAMED_ENTITIES[body.lowercase()] ?: match.value
            }
        }
    }

    private fun decode(bytes: ByteArray): String = bytes.toString(Charsets.UTF_8)

    private fun baseName(name: String): String {
        val file = name.replace('\\', '/').substringAfterLast('/')
        return file.substringBeforeLast('.', file).trim().ifBlank { "untitled" }
    }
}

/**
 * Converts Keep metadata into Folio's model. The deterministic UUID makes a second
 * import of the same export a no-op instead of a duplicate library.
 */
fun KeepNote.asFolioNote(now: Long = System.currentTimeMillis()): Note {
    val created = createdAt.takeIf { it > 0L } ?: now
    val updated = updatedAt.takeIf { it > 0L }?.coerceAtLeast(created) ?: created
    val fingerprint = "$sourceId\u0000$title\u0000$body"
    return Note(
        id = UUID.nameUUIDFromBytes(fingerprint.toByteArray(Charsets.UTF_8)).toString(),
        title = title.trim(),
        body = body.trim(),
        tint = tint,
        pinned = pinned,
        archived = archived,
        trashed = trashed,
        tags = labels.distinctBy { it.lowercase() },
        createdAt = created,
        updatedAt = updated,
    )
}
