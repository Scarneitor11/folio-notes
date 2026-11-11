package com.folio.notes.logic

/** A text change plus the caret/selection that should follow it. */
data class TextEdit(val text: String, val start: Int, val end: Int) {
    val caret: Int get() = end
}

/**
 * Pure text operations behind the editor toolbar. Kept free of Compose types so
 * they can be tested on the JVM without an Android runtime.
 */
object EditorOps {

    fun wrap(text: String, selStart: Int, selEnd: Int, marker: String): TextEdit {
        val lo = minOf(selStart, selEnd).coerceIn(0, text.length)
        val hi = maxOf(selStart, selEnd).coerceIn(0, text.length)
        val selected = text.substring(lo, hi)
        val next = buildString {
            append(text, 0, lo)
            append(marker)
            append(selected)
            append(marker)
            append(text, hi, text.length)
        }
        return TextEdit(next, lo + marker.length, hi + marker.length)
    }

    fun lineStart(text: String, offset: Int): Int {
        if (offset <= 0) return 0
        val index = text.lastIndexOf('\n', offset - 1)
        return if (index < 0) 0 else index + 1
    }

    fun lineEnd(text: String, offset: Int): Int {
        val index = text.indexOf('\n', offset.coerceIn(0, text.length))
        return if (index < 0) text.length else index
    }

    /** Adds or removes a prefix (a heading marker, a bullet, a quote) on every touched line. */
    fun toggleLinePrefix(text: String, selStart: Int, selEnd: Int, prefix: String): TextEdit {
        val lo = minOf(selStart, selEnd).coerceIn(0, text.length)
        val hi = maxOf(selStart, selEnd).coerceIn(0, text.length)
        val blockStart = lineStart(text, lo)
        val blockEnd = lineEnd(text, hi.coerceAtLeast(lo))
        val block = text.substring(blockStart, blockEnd)
        val lines = block.split('\n')
        val filled = lines.filter { it.isNotBlank() }
        val remove = filled.isNotEmpty() && filled.all { it.startsWith(prefix) }

        val mapped = lines.joinToString("\n") { line ->
            when {
                line.isBlank() -> line
                remove -> line.removePrefix(prefix)
                else -> prefix + line
            }
        }
        val next = text.substring(0, blockStart) + mapped + text.substring(blockEnd)
        val delta = mapped.length - block.length
        val caretShift = if (remove) -prefix.length else prefix.length
        val newStart = (lo + caretShift).coerceIn(blockStart, blockStart + mapped.length)
        val newEnd = (hi + delta).coerceIn(newStart, blockStart + mapped.length)
        return TextEdit(next, newStart, newEnd)
    }

    /**
     * `- [ ]` ↔ `- [x]`, adding the marker when the line has none. Leading
     * whitespace is kept, so tasks nested inside a list keep their indent.
     */
    fun toggleTask(text: String, selStart: Int, selEnd: Int): TextEdit {
        val caret = selEnd.coerceIn(0, text.length)
        val start = lineStart(text, caret)
        val end = lineEnd(text, caret)
        val line = text.substring(start, end)
        val indent = line.takeWhile { it == ' ' || it == '\t' }
        val content = line.substring(indent.length)
        val lower = content.lowercase()
        val open = "- [ ] "
        val done = "- [x] "

        val replacement = when {
            lower.startsWith("- [x] ") -> open + content.substring(6)
            lower.startsWith("- [ ] ") -> done + content.substring(6)
            content.startsWith("- ") || content.startsWith("* ") || content.startsWith("+ ") ->
                open + content.substring(2)
            else -> open + content.trimStart()
        }
        val updated = indent + replacement
        val next = text.substring(0, start) + updated + text.substring(end)
        val position = start + updated.length
        return TextEdit(next, position, position)
    }

    fun insertBlock(text: String, selStart: Int, selEnd: Int, snippet: String): TextEdit {
        val caret = selEnd.coerceIn(0, text.length)
        val start = lineStart(text, caret)
        val needsBreak = start > 0 && text[start - 1] != '\n'
        val insertion = (if (needsBreak) "\n" else "") + snippet
        val next = text.substring(0, start) + insertion + text.substring(start)
        val position = start + insertion.length
        return TextEdit(next, position, position)
    }

    fun indent(text: String, selStart: Int, selEnd: Int): TextEdit {
        val lo = minOf(selStart, selEnd).coerceIn(0, text.length)
        val hi = maxOf(selStart, selEnd).coerceIn(0, text.length)
        val blockStart = lineStart(text, lo)
        val blockEnd = lineEnd(text, hi)
        val block = text.substring(blockStart, blockEnd)
        val lines = block.split('\n').map { line ->
            when {
                line.isBlank() -> line
                line.startsWith("  ") -> line.removePrefix("  ")
                else -> "  $line"
            }
        }
        val mapped = lines.joinToString("\n")
        val next = text.substring(0, blockStart) + mapped + text.substring(blockEnd)
        val delta = mapped.length - block.length
        return TextEdit(
            next,
            (lo + delta).coerceIn(blockStart, blockStart + mapped.length),
            (hi + delta).coerceIn(blockStart, blockStart + mapped.length),
        )
    }

    data class Replacement(val text: String, val count: Int)

    /** The edited text and the match that should become current next (-1 if none). */
    data class ReplaceStep(val text: String, val nextIndex: Int)

    /** A literal match in character offsets, used by previous/next navigation. */
    data class MatchRange(val start: Int, val end: Int)

    /**
     * Non-overlapping literal matches in document order. The replace pass and the
     * previous/next navigation share this scan, so their counts cannot disagree.
     */
    fun matchRanges(text: String, query: String, matchCase: Boolean): List<MatchRange> {
        if (query.isEmpty()) return emptyList()
        val found = ArrayList<MatchRange>()
        var scan = 0
        // Compare in place rather than lowercasing first: some glyphs change length
        // when case-folded (İ -> i̇), which would shift every later offset.
        while (scan <= text.length - query.length) {
            if (text.regionMatches(scan, query, 0, query.length, ignoreCase = !matchCase)) {
                found += MatchRange(scan, scan + query.length)
                scan += query.length
            } else {
                scan++
            }
        }
        return found
    }

    /**
     * Literal find & replace. Regex characters are matched as plain text, which is what
     * someone editing prose expects. An empty query is a no-op so the live match counter
     * can call this on every keystroke.
     */
    fun replacement(
        text: String,
        query: String,
        replacement: String,
        matchCase: Boolean,
    ): Replacement {
        val ranges = matchRanges(text, query, matchCase)
        if (ranges.isEmpty()) return Replacement(text, 0)
        val out = StringBuilder(text.length)
        var cursor = 0
        for (range in ranges) {
            out.append(text, cursor, range.start)
            out.append(replacement)
            cursor = range.end
        }
        out.append(text, cursor, text.length)
        return Replacement(out.toString(), ranges.size)
    }

    /**
     * Replaces one match and reports which match should become current next.
     * The next index is resolved against the edited text, and points at the next
     * original match rather than a match introduced inside [replacement]. That
     * keeps repeated Replace actions advancing when the replacement contains the
     * query. Returns null when [index] is not a current match.
     */
    fun replaceMatch(
        text: String,
        query: String,
        replacement: String,
        matchCase: Boolean,
        index: Int,
    ): ReplaceStep? {
        val ranges = matchRanges(text, query, matchCase)
        if (index !in ranges.indices) return null
        val target = ranges[index]
        val nextText = text.replaceRange(target.start, target.end, replacement)
        val following = if (ranges.size > 1) ranges[(index + 1) % ranges.size] else null
        if (following == null) return ReplaceStep(nextText, -1)
        val delta = replacement.length - (target.end - target.start)
        val expectedStart = if (following.start >= target.end) {
            following.start + delta
        } else {
            following.start
        }
        val after = matchRanges(nextText, query, matchCase)
        return ReplaceStep(nextText, after.indexOfFirst { it.start == expectedStart })
    }

    /**
     * Pressing Enter at the end of a list or quote line continues that structure on the
     * next line. Pressing it on an empty item leaves the list instead of adding another
     * marker. Cursor positions are treated as a caret, so malformed ranges cannot crash.
     */
    fun continueList(text: String, caret: Int): TextEdit {
        val pos = caret.coerceIn(0, text.length)
        if (pos == 0 || text[pos - 1] != '\n') return TextEdit(text, pos, pos)
        val previous = text.lastIndexOf('\n', pos - 2)
        val lineStart = if (previous < 0) 0 else previous + 1
        val line = text.substring(lineStart, pos - 1)

        TASK_LINE.matchEntire(line)?.let { match ->
            return continueLine(
                text = text,
                pos = pos,
                lineStart = lineStart,
                prefix = match.groupValues[1] + match.groupValues[2] + " [ ] ",
                body = match.groupValues[3],
            )
        }
        ORDERED_LINE.matchEntire(line)?.let { match ->
            val next = match.groupValues[2].toIntOrNull()?.plus(1) ?: return TextEdit(text, pos, pos)
            return continueLine(
                text = text,
                pos = pos,
                lineStart = lineStart,
                prefix = match.groupValues[1] + next + match.groupValues[3] + " ",
                body = match.groupValues[4],
            )
        }
        BULLET_LINE.matchEntire(line)?.let { match ->
            return continueLine(
                text = text,
                pos = pos,
                lineStart = lineStart,
                prefix = match.groupValues[1] + match.groupValues[2] + " ",
                body = match.groupValues[3],
            )
        }
        QUOTE_LINE.matchEntire(line)?.let { match ->
            return continueLine(
                text = text,
                pos = pos,
                lineStart = lineStart,
                prefix = match.groupValues[1] + "> ",
                body = match.groupValues[2],
            )
        }
        return TextEdit(text, pos, pos)
    }

    private fun continueLine(
        text: String,
        pos: Int,
        lineStart: Int,
        prefix: String,
        body: String,
    ): TextEdit {
        if (body.isBlank()) {
            val next = text.substring(0, lineStart) + text.substring(pos)
            return TextEdit(next, lineStart, lineStart)
        }
        val next = text.substring(0, pos) + prefix + text.substring(pos)
        return TextEdit(next, pos + prefix.length, pos + prefix.length)
    }

    data class TemplateInsertion(val title: String, val body: String)

    /**
     * Applies a template without replacing authored content. A blank title takes
     * the template name, and an existing body keeps its text followed by one
     * blank line before the template.
     */
    fun insertTemplate(
        title: String,
        body: String,
        templateName: String,
        templateBody: String,
    ): TemplateInsertion {
        val nextTitle = title.ifBlank { templateName }
        val nextBody = if (body.isBlank()) {
            templateBody
        } else {
            body.trimEnd() + "\n\n" + templateBody
        }
        return TemplateInsertion(nextTitle, nextBody)
    }

    private val TASK_LINE = Regex("^(\\s*)([-*+]) \\[[ xX]\\] (.*)$")
    private val ORDERED_LINE = Regex("^(\\s*)(\\d+)([.)]) (.*)$")
    private val BULLET_LINE = Regex("^(\\s*)([-*+]) (.*)$")
    private val QUOTE_LINE = Regex("^(\\s*)> ?(.*)$")

    data class Stats(val words: Int, val characters: Int, val minutes: Int)

    fun stats(text: String): Stats {
        val words = NoteQuery.words(text)
        val minutes = NoteQuery.readingMinutes(words)
        return Stats(words = words, characters = text.length, minutes = minutes)
    }
}
