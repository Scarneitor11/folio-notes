package com.folio.notes.logic

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.zip.ZipInputStream

/**
 * Imports Markdown written by Obsidian, Bear, Standard Notes, or any editor that
 * uses a small YAML front matter block. Folio keeps the Markdown source intact;
 * only the front matter and a leading H1 are lifted into note metadata.
 */
object MarkdownImport {

    private val H1 = Regex("(?m)^#\\s+(.+?)\\s*$")
    private val DATE_FORMATS = listOf(
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd",
    )
    private const val MAX_ENTRY_BYTES = 8 * 1024 * 1024
    private const val MAX_ENTRIES = 4_000

    /** Reads a Markdown vault ZIP, such as an Obsidian export, entry by entry. */
    fun parseZip(input: InputStream): KeepImportResult {
        val notes = mutableListOf<KeepNote>()
        var filesRead = 0
        var skipped = 0
        runCatching {
            ZipInputStream(input).use { zip ->
                while (notes.size < MAX_ENTRIES) {
                    val entry = zip.nextEntry ?: break
                    val path = entry.name.replace('\\', '/')
                    val lower = path.lowercase(Locale.ROOT)
                    val noteFile = !entry.isDirectory &&
                        (lower.endsWith(".md") || lower.endsWith(".markdown")) &&
                        !lower.startsWith(".obsidian/") &&
                        !lower.contains("/.obsidian/") &&
                        !lower.contains("__macosx/") &&
                        !lower.substringAfterLast('/').startsWith(".")
                    if (!noteFile) {
                        skipped++
                        zip.closeEntry()
                        continue
                    }
                    val bytes = readEntry(zip, MAX_ENTRY_BYTES)
                    if (bytes == null) {
                        skipped++
                    } else {
                        filesRead++
                        val parsed = parse(path, bytes.toString(Charsets.UTF_8))
                        if (parsed.notes.isEmpty()) skipped++ else notes += parsed.notes
                    }
                    zip.closeEntry()
                }
            }
        }.onFailure { return KeepImportResult(emptyList(), filesRead, skipped + 1) }
        return KeepImportResult(notes, filesRead, skipped)
    }

    private fun readEntry(input: InputStream, maxBytes: Int): ByteArray? {
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

    fun parse(name: String, raw: String): KeepImportResult {
        if (raw.isBlank()) return KeepImportResult(emptyList(), 0, 1)
        return runCatching {
            val normalized = raw.removePrefix("\uFEFF").replace("\r\n", "\n")
            val (front, bodyRaw) = splitFrontMatter(normalized)
            var body = bodyRaw.trim()
            val heading = H1.find(body)
            val headingTitle = heading?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }
            val explicitTitle = front["title"]?.trim()?.takeIf { it.isNotEmpty() }
            val title = explicitTitle ?: headingTitle ?: baseName(name)
            // A leading H1 is the title, so keeping it in the body would show the
            // same sentence twice in Folio's title-and-body editor. Do this even
            // when front matter supplied a title; exports often repeat it.
            if (heading != null) {
                body = body.removeRange(heading.range).trimStart('\n').trim()
            }
            val tags = parseTags(front["tags"] ?: front["tag"])
            val created = parseDate(front["created"] ?: front["date"])
            val updated = parseDate(front["updated"] ?: front["modified"])
            if (title.isBlank() && body.isBlank()) {
                KeepImportResult(emptyList(), 0, 1)
            } else {
                KeepImportResult(
                    notes = listOf(
                        KeepNote(
                            sourceId = "markdown:$name",
                            title = title,
                            body = body,
                            labels = tags,
                            createdAt = created,
                            updatedAt = updated,
                        ),
                    ),
                    filesRead = 1,
                    skipped = 0,
                )
            }
        }.getOrElse { KeepImportResult(emptyList(), 0, 1) }
    }

    private fun splitFrontMatter(text: String): Pair<Map<String, String>, String> {
        if (!text.startsWith("---\n")) return emptyMap<String, String>() to text
        val lines = text.split('\n')
        val end = lines.drop(1).indexOfFirst { it.trim() == "---" }
        if (end < 0) return emptyMap<String, String>() to text
        val values = mutableMapOf<String, String>()
        var listKey: String? = null
        for (line in lines.subList(1, end + 1)) {
            if (line.isBlank()) continue
            val trimmed = line.trim()
            if (trimmed.startsWith("- ")) {
                val key = listKey ?: continue
                val item = trimmed.removePrefix("- ").trim().trim('"', '\'')
                values[key] = listOf(values[key], item).filterNotNull().joinToString(", ")
                continue
            }
            val separator = line.indexOf(':')
            if (separator <= 0) continue
            val key = line.substring(0, separator).trim().lowercase()
            val value = line.substring(separator + 1).trim().trim('"', '\'')
            if (value.isEmpty()) {
                listKey = key
            } else {
                values[key] = value
                listKey = null
            }
        }
        val body = lines.drop(end + 2).joinToString("\n")
        return values to body
    }

    private fun parseTags(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        val normalized = raw
            .trim()
            .removeSurrounding("[", "]")
            .removeSurrounding("(", ")")
        // Commas delimit tags and preserve quoted spaces ("long form"). Only
        // fall back to whitespace for a plain, comma-free tag line.
        val pieces = if (normalized.contains(',')) {
            normalized.split(',')
        } else {
            normalized.split(' ', ';', '\n')
        }
        return pieces
            .map { it.trim().trim('"', '\'').removePrefix("#") }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }
            .take(100)
    }

    private fun parseDate(raw: String?): Long {
        if (raw.isNullOrBlank()) return 0L
        val value = raw.trim().trim('"', '\'')
        for (pattern in DATE_FORMATS) {
            val format = SimpleDateFormat(pattern, Locale.US).apply {
                isLenient = false
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val parsed = runCatching { format.parse(value) }.getOrNull()
            if (parsed != null) return parsed.time
        }
        return 0L
    }

    private fun baseName(name: String): String {
        val file = name.replace('\\', '/').substringAfterLast('/')
        return file.substringBeforeLast('.', file).trim().ifBlank { "Untitled" }
    }
}
