package com.folio.notes.logic

import com.folio.notes.data.Feed
import com.folio.notes.data.Note
import com.folio.notes.data.SortMode
import kotlin.math.abs

/** Pure query helpers so filtering and ordering can be unit tested. */
object NoteQuery {

    fun visible(
        notes: List<Note>,
        feed: Feed,
        sort: SortMode,
        query: String = "",
        tag: String? = null,
    ): List<Note> {
        val needle = query.trim().lowercase()
        val filtered = notes.filter { note ->
            val feedOk = when (feed) {
                Feed.ALL -> !note.trashed && !note.archived
                Feed.PINNED -> !note.trashed && !note.archived && note.pinned
                Feed.ARCHIVED -> !note.trashed && note.archived
                Feed.TRASH -> note.trashed
            }
            if (!feedOk) return@filter false
            if (tag != null && note.tags.none { it.equals(tag, ignoreCase = true) }) return@filter false
            if (needle.isEmpty()) return@filter true
            note.title.lowercase().contains(needle) ||
                note.body.lowercase().contains(needle) ||
                note.tags.any { it.lowercase().contains(needle) }
        }
        val sorted = sorted(filtered, sort)
        if (feed == Feed.ALL || feed == Feed.PINNED) {
            return sorted.filter { it.pinned } + sorted.filterNot { it.pinned }
        }
        return sorted
    }

    fun sorted(notes: List<Note>, sort: SortMode): List<Note> = when (sort) {
        SortMode.UPDATED_DESC -> notes.sortedByDescending { it.updatedAt }
        SortMode.UPDATED_ASC -> notes.sortedBy { it.updatedAt }
        SortMode.CREATED_DESC -> notes.sortedByDescending { it.createdAt }
        SortMode.TITLE_ASC -> notes.sortedBy { titleOf(it).lowercase() }
        SortMode.TITLE_DESC -> notes.sortedByDescending { titleOf(it).lowercase() }
    }

    fun titleOf(note: Note): String =
        note.title.trim().ifBlank {
            val index = titleLineIndex(note.body)
            if (index < 0) "Untitled" else lineText(note.body.lines()[index])
        }

    fun previewOf(note: Note, maxChars: Int = 220): String {
        val source = if (note.title.isBlank()) {
            val index = titleLineIndex(note.body)
            if (index < 0) "" else note.body.lines().drop(index + 1).joinToString("\n")
        } else {
            note.body
        }
        return MarkdownLite.plainText(source).trim().take(maxChars).trim()
    }

    /** Index of the first line that carries readable text — blanks and thematic breaks are skipped. */
    private fun titleLineIndex(body: String): Int = body.lines().indexOfFirst { raw ->
        val line = raw.trim()
        line.isNotEmpty() && !(line.length >= 3 && line.all { it == '-' || it == '*' || it == '_' })
    }

    /** Strips block markers and inline styling so a raw source line reads as a title. */
    private fun lineText(raw: String): String {
        val line = raw.trim()
        val withoutMarker = when {
            line.startsWith("#") -> line.dropWhile { it == '#' }.trimStart()
            line.startsWith("- [ ] ") -> line.removePrefix("- [ ] ")
            line.startsWith("- [x] ") -> line.removePrefix("- [x] ")
            line.startsWith("- [X] ") -> line.removePrefix("- [X] ")
            line.startsWith("- ") || line.startsWith("* ") || line.startsWith("+ ") ||
                line.startsWith("> ") -> line.drop(2).trimStart()
            else -> line
        }
        return MarkdownLite.inline(withoutMarker)
            .joinToString("") { it.text }
            .trim()
            .ifBlank { "Untitled" }
    }

    fun words(text: String): Int =
        text.split(' ', '\n', '\t')
            .count { it.any { ch -> ch.isLetterOrDigit() } }

    fun readingMinutes(text: String): Int = readingMinutes(words(text))

    /** Overload for callers that already counted words, avoiding a second scan. */
    fun readingMinutes(wordCount: Int): Int {
        if (wordCount == 0) return 0
        return ((wordCount / 200f).coerceAtLeast(0.4f)).toInt().coerceAtLeast(1)
    }

    fun allTags(notes: List<Note>): List<Pair<String, Int>> =
        notes.filterNot { it.trashed }
            .flatMap { it.tags }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key.lowercase() })
            .map { it.key to it.value }

    fun activeCount(notes: List<Note>): Int = notes.count { !it.trashed && !it.archived }

    /** "2m", "just now", "Yesterday", "14 Mar" — short enough for a card footer. */
    fun relativeTime(now: Long, then: Long): String {
        if (then <= 0L) return ""
        val delta = now - then
        if (delta < 0) return "just now"
        val minutes = delta / 60_000L
        val hours = delta / 3_600_000L
        val days = delta / 86_400_000L
        return when {
            minutes < 1 -> "just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            days == 1L -> "yesterday"
            days < 7 -> "${days}d ago"
            days < 30 -> "${days / 7}w ago"
            else -> {
                val months = listOf(
                    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
                )
                val calendar = java.util.Calendar.getInstance().apply { timeInMillis = then }
                val month = months[calendar.get(java.util.Calendar.MONTH)]
                val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)
                val year = calendar.get(java.util.Calendar.YEAR)
                val thisYear = java.util.Calendar.getInstance().apply { timeInMillis = now }
                    .get(java.util.Calendar.YEAR)
                if (year == thisYear) "$day $month" else "$day $month $year"
            }
        }
    }

    fun sameDay(a: Long, b: Long): Boolean {
        val ca = java.util.Calendar.getInstance().apply { timeInMillis = a }
        val cb = java.util.Calendar.getInstance().apply { timeInMillis = b }
        return ca.get(java.util.Calendar.YEAR) == cb.get(java.util.Calendar.YEAR) &&
            ca.get(java.util.Calendar.DAY_OF_YEAR) == cb.get(java.util.Calendar.DAY_OF_YEAR)
    }

    fun nearlyEqual(a: Long, b: Long, tolerance: Long = 1L): Boolean = abs(a - b) <= tolerance
}
